package br.com.balcao.pdv.fiscal;

import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.venda.ItemVenda;
import br.com.balcao.pdv.venda.StatusVenda;
import br.com.balcao.pdv.venda.Venda;
import br.com.balcao.pdv.venda.VendaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NfceService {

    private static final String EMISSAO_NORMAL = "1";

    private final NotaFiscalRepository repository;
    private final VendaRepository vendaRepository;
    private final ConfiguracaoFiscalService configuracaoService;
    private final Map<TipoEmissor, EmissorNfce> emissores;
    private final TransactionTemplate transacao;
    private final Clock relogio;

    public NfceService(NotaFiscalRepository repository, VendaRepository vendaRepository,
                       ConfiguracaoFiscalService configuracaoService, List<EmissorNfce> emissores,
                       PlatformTransactionManager transactionManager, Clock relogio) {
        this.repository = repository;
        this.vendaRepository = vendaRepository;
        this.configuracaoService = configuracaoService;
        this.emissores = emissores.stream().collect(Collectors.toMap(EmissorNfce::tipo, Function.identity()));
        this.transacao = new TransactionTemplate(transactionManager);
        this.relogio = relogio;
    }

    /** Emite (ou reemite) a NFC-e de uma venda finalizada. */
    public NotaFiscalResumo emitir(Long vendaId) {
        return transacao.execute(status -> NotaFiscalResumo.de(emitirNaTransacao(vendaId)));
    }

    /**
     * Chamado depois de finalizar a venda. Nunca lança exceção: a venda já está gravada (RN-NFC-09).
     *
     * @return aviso para o operador quando a nota não foi autorizada
     */
    public Optional<String> emitirSeAutomatico(Long vendaId) {
        if (!configuracaoService.obter().isEmissaoAutomatica()) {
            return Optional.empty();
        }
        try {
            NotaFiscalResumo nota = emitir(vendaId);
            if (nota.status() == StatusNota.AUTORIZADA) {
                return Optional.empty();
            }
            return Optional.of("NFC-e " + nota.status().name().toLowerCase() + ": " + nota.motivo());
        } catch (RegraNegocioException e) {
            return Optional.of("NFC-e não emitida: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("Falha inesperada ao emitir NFC-e da venda #{}", vendaId, e);
            return Optional.of("NFC-e não emitida: erro inesperado. Tente reemitir pela tela Fiscal.");
        }
    }

    private NotaFiscal emitirNaTransacao(Long vendaId) {
        Venda venda = vendaRepository.findById(vendaId).orElseThrow(() -> new NaoEncontradoException("Venda", vendaId));
        if (venda.getStatus() != StatusVenda.FINALIZADA) {
            throw new RegraNegocioException("VENDA_NAO_FINALIZADA", "Só vendas finalizadas podem ter NFC-e.");
        }
        ConfiguracaoFiscal config = configuracaoService.travar();
        configuracaoService.validarCompleta(config);
        venda.getItens().stream().map(ItemVenda::getProduto).forEach(NfceXmlBuilder::validarProduto);

        // Rejeitada ou pendente: reaproveita o número (não houve autorização). Senão, número novo.
        NotaFiscal anterior = repository.findFirstByVendaIdOrderByIdDesc(vendaId).orElse(null);
        if (anterior != null && anterior.getStatus() == StatusNota.AUTORIZADA) {
            throw new ConflitoException("NOTA_JA_AUTORIZADA", "Esta venda já tem NFC-e autorizada.",
                    Map.of("notaId", anterior.getId()));
        }
        boolean reaproveitar = anterior != null
                && (anterior.getStatus() == StatusNota.REJEITADA || anterior.getStatus() == StatusNota.PENDENTE)
                && anterior.getAmbiente() == config.getAmbiente();
        NotaFiscal nota = reaproveitar ? anterior
                : new NotaFiscal(venda, config.getSerie(), config.reservarNumero(), config.getAmbiente());

        OffsetDateTime emissao = OffsetDateTime.now(relogio).withNano(0);
        String codigoNumerico = codigoNumerico(nota.getNumero());
        String chave = ChaveAcesso.gerar(config.getUf(), emissao, config.getCnpj(), NotaFiscal.MODELO_NFCE,
                nota.getSerie(), nota.getNumero(), EMISSAO_NORMAL, codigoNumerico);
        String urlQr = QrCodeNfce.url(config.getUrlQrCode(), chave, nota.getAmbiente(), config.getCscId(),
                config.getCsc());

        nota.setChaveAcesso(chave);
        nota.setDataEmissao(emissao);
        nota.setUrlQrCode(urlQr);
        nota.setXml(NfceXmlBuilder.montar(
                new NfceXmlBuilder.Dados(config, venda, nota, codigoNumerico, emissao, urlQr)));

        EmissorNfce emissor = emissor(config);
        try {
            EmissorNfce.Retorno retorno = emissor.autorizar(nota, nota.getXml(), config);
            nota.setMotivo(retorno.codigoStatus() + " - " + retorno.motivo());
            if (retorno.sucesso()) {
                nota.setStatus(StatusNota.AUTORIZADA);
                nota.setProtocolo(retorno.protocolo());
                nota.setDataAutorizacao(retorno.dataHora());
            } else {
                nota.setStatus(StatusNota.REJEITADA);
            }
        } catch (RuntimeException e) {
            log.warn("Falha na transmissão da NFC-e {} da venda #{}: {}", nota.getNumero(), vendaId, e.getMessage());
            nota.setStatus(StatusNota.PENDENTE);
            nota.setMotivo("Falha na transmissão: " + e.getMessage());
        }
        log.info("NFC-e série {} nº {} da venda #{}: {}", nota.getSerie(), nota.getNumero(), vendaId, nota.getStatus());
        return repository.save(nota);
    }

    @Transactional
    public NotaFiscalResumo cancelar(Long notaId, String justificativa) {
        String texto = justificativa == null ? "" : justificativa.trim();
        if (texto.length() < 15 || texto.length() > 255) {
            throw new RegraNegocioException("JUSTIFICATIVA_INVALIDA",
                    "A justificativa do cancelamento deve ter de 15 a 255 caracteres.");
        }
        NotaFiscal nota = buscar(notaId);
        if (nota.getStatus() != StatusNota.AUTORIZADA) {
            throw new ConflitoException("NOTA_NAO_AUTORIZADA", "Só notas autorizadas podem ser canceladas.");
        }
        ConfiguracaoFiscal config = configuracaoService.obter();
        OffsetDateTime limite = nota.getDataAutorizacao().plusMinutes(config.getPrazoCancelamentoMin());
        if (OffsetDateTime.now(relogio).isAfter(limite)) {
            throw new RegraNegocioException("PRAZO_CANCELAMENTO_EXPIRADO",
                    "O prazo de cancelamento (" + config.getPrazoCancelamentoMin() + " min) já passou.");
        }
        EmissorNfce.Retorno retorno = emissor(config).cancelar(nota, texto, config);
        if (!retorno.sucesso()) {
            throw new RegraNegocioException("CANCELAMENTO_REJEITADO",
                    "SEFAZ recusou o cancelamento: " + retorno.codigoStatus() + " - " + retorno.motivo());
        }
        nota.setStatus(StatusNota.CANCELADA);
        nota.setJustificativaCancelamento(texto);
        nota.setProtocoloCancelamento(retorno.protocolo());
        nota.setDataCancelamento(retorno.dataHora());
        log.info("NFC-e nº {} cancelada", nota.getNumero());
        return NotaFiscalResumo.de(nota);
    }

    @Transactional(readOnly = true)
    public Page<NotaFiscalResumo> pesquisar(OffsetDateTime inicio, OffsetDateTime fim, StatusNota status,
                                            Pageable pageable) {
        return repository.pesquisar(inicio, fim, status, pageable).map(NotaFiscalResumo::de);
    }

    @Transactional(readOnly = true)
    public NotaFiscalResumo resumo(Long id) {
        return NotaFiscalResumo.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public NotaFiscal comXml(Long id) {
        return buscar(id);
    }

    @Transactional(readOnly = true)
    public DanfeNfce danfe(Long id) {
        return DanfeNfce.de(buscar(id), configuracaoService.obter());
    }

    private NotaFiscal buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NaoEncontradoException("Nota", id));
    }

    private EmissorNfce emissor(ConfiguracaoFiscal config) {
        EmissorNfce emissor = emissores.get(config.getEmissor());
        if (emissor == null) {
            throw new RegraNegocioException("EMISSOR_INDISPONIVEL", "Emissor " + config.getEmissor() + " indisponível.");
        }
        return emissor;
    }

    /** cNF: 8 dígitos aleatórios, diferente do número da nota. */
    private static String codigoNumerico(int numero) {
        String numeroFormatado = String.format("%08d", numero % 100_000_000);
        String codigo;
        do {
            codigo = String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        } while (codigo.equals(numeroFormatado));
        return codigo;
    }
}
