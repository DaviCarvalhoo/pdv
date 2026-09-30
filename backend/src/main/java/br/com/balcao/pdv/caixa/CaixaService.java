package br.com.balcao.pdv.caixa;

import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.venda.FormaPagamento;
import br.com.balcao.pdv.venda.StatusVenda;
import br.com.balcao.pdv.venda.VendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaixaService {

    private final CaixaRepository repository;
    private final MovimentacaoCaixaRepository movimentacaoRepository;
    private final VendaRepository vendaRepository;
    private final Clock relogio;

    @Transactional
    public Caixa abrir(BigDecimal saldoInicial) {
        return abrir(saldoInicial, null);
    }

    @Transactional
    public Caixa abrir(BigDecimal saldoInicial, Long operadorId) {
        if (saldoInicial == null || saldoInicial.signum() < 0) {
            throw new RegraNegocioException("SALDO_INICIAL_INVALIDO", "O saldo inicial não pode ser negativo.");
        }
        repository.findFirstByStatus(StatusCaixa.ABERTO).ifPresent(c -> {
            throw new ConflitoException("CAIXA_JA_ABERTO", "Já existe um caixa aberto (#" + c.getId() + ").",
                    Map.of("caixaId", c.getId()));
        });
        try {
            Caixa caixa = repository.saveAndFlush(new Caixa(Dinheiro.valor(saldoInicial), agora(), operadorId));
            log.info("Caixa #{} aberto com saldo inicial {}", caixa.getId(), caixa.getSaldoInicial());
            return caixa;
        } catch (DataIntegrityViolationException e) {
            // Índice único parcial: outra requisição abriu um caixa ao mesmo tempo.
            throw new ConflitoException("CAIXA_JA_ABERTO", "Já existe um caixa aberto.");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Caixa> aberto() {
        return repository.findFirstByStatus(StatusCaixa.ABERTO);
    }

    @Transactional(readOnly = true)
    public Caixa exigirAberto() {
        return aberto().orElseThrow(() ->
                new RegraNegocioException("CAIXA_NAO_ABERTO", "Nenhum caixa aberto. Abra o caixa primeiro."));
    }

    @Transactional(readOnly = true)
    public Caixa buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NaoEncontradoException("Caixa", id));
    }

    /** Trava o caixa até o fim da transação: evita fechar o caixa enquanto uma venda é finalizada. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Caixa travar(Long id) {
        return repository.travarPorId(id).orElseThrow(() -> new NaoEncontradoException("Caixa", id));
    }

    @Transactional(readOnly = true)
    public Page<Caixa> porPeriodo(OffsetDateTime inicio, OffsetDateTime fim, Pageable pageable) {
        return repository.porPeriodo(inicio, fim, pageable);
    }

    @Transactional
    public MovimentacaoCaixa suprimento(Long caixaId, BigDecimal valor, String descricao) {
        return suprimento(caixaId, valor, descricao, null);
    }

    @Transactional
    public MovimentacaoCaixa suprimento(Long caixaId, BigDecimal valor, String descricao, Long operadorId) {
        Caixa caixa = travarAberto(caixaId);
        exigirPositivo(valor);
        return registrar(caixa, TipoMovimentacaoCaixa.SUPRIMENTO, valor, texto(descricao, "Suprimento"), null,
                operadorId);
    }

    @Transactional
    public MovimentacaoCaixa sangria(Long caixaId, BigDecimal valor, String descricao) {
        return sangria(caixaId, valor, descricao, null);
    }

    @Transactional
    public MovimentacaoCaixa sangria(Long caixaId, BigDecimal valor, String descricao, Long operadorId) {
        Caixa caixa = travarAberto(caixaId);
        exigirPositivo(valor);
        BigDecimal saldo = saldoEsperado(caixa);
        if (Dinheiro.valor(valor).compareTo(saldo) > 0) {
            throw new RegraNegocioException("SANGRIA_EXCEDE_SALDO",
                    "A sangria não pode ser maior que o saldo em dinheiro do caixa (" + saldo + ").",
                    Map.of("saldoEsperado", saldo));
        }
        return registrar(caixa, TipoMovimentacaoCaixa.SANGRIA, valor, texto(descricao, "Sangria"), null,
                operadorId);
    }

    /** Entrada do dinheiro líquido (recebido − troco) de uma venda finalizada. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarVendaDinheiro(Caixa caixa, BigDecimal valor, Long vendaId, Long operadorId) {
        if (Dinheiro.positivo(valor)) {
            registrar(caixa, TipoMovimentacaoCaixa.VENDA_DINHEIRO, valor, "Venda #" + vendaId, vendaId, operadorId);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarEstornoVenda(Caixa caixa, BigDecimal valor, Long vendaId, Long operadorId) {
        if (Dinheiro.positivo(valor)) {
            registrar(caixa, TipoMovimentacaoCaixa.ESTORNO_VENDA, valor, "Estorno da venda #" + vendaId, vendaId,
                    operadorId);
        }
    }

    /** Dinheiro devolvido ao cliente numa devolução (sai da gaveta do caixa aberto). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarDevolucao(Caixa caixa, BigDecimal valor, Long vendaId, Long operadorId) {
        caixa.exigirAberto();
        BigDecimal saldo = saldoEsperado(caixa);
        if (Dinheiro.valor(valor).compareTo(saldo) > 0) {
            throw new RegraNegocioException("SALDO_INSUFICIENTE",
                    "Não há dinheiro suficiente na gaveta (" + saldo + "). Use o vale-troca.");
        }
        registrar(caixa, TipoMovimentacaoCaixa.ESTORNO_VENDA, valor, "Devolução da venda #" + vendaId, vendaId,
                operadorId);
    }

    /** Fiado recebido em dinheiro entra na gaveta. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarRecebimentoCliente(Caixa caixa, BigDecimal valor, String cliente, Long operadorId) {
        registrar(caixa, TipoMovimentacaoCaixa.RECEBIMENTO_CLIENTE, valor, "Fiado de " + cliente, null, operadorId);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoCaixa> movimentacoes(Long caixaId) {
        buscar(caixaId);
        return movimentacaoRepository.findByCaixaIdOrderByDataHoraAscIdAsc(caixaId);
    }

    /** RN-CX-02: saldo inicial + suprimentos + vendas em dinheiro − sangrias − estornos. */
    @Transactional(readOnly = true)
    public BigDecimal saldoEsperado(Caixa caixa) {
        BigDecimal saldo = caixa.getSaldoInicial();
        for (TipoMovimentacaoCaixa tipo : TipoMovimentacaoCaixa.values()) {
            BigDecimal soma = movimentacaoRepository.somaPorTipo(caixa.getId(), tipo);
            saldo = tipo.sinal() > 0 ? saldo.add(soma) : saldo.subtract(soma);
        }
        return Dinheiro.valor(saldo);
    }

    @Transactional
    public ExtratoCaixa fechar(Long caixaId, BigDecimal valorContado) {
        return fechar(caixaId, valorContado, null);
    }

    @Transactional
    public ExtratoCaixa fechar(Long caixaId, BigDecimal valorContado, Long operadorId) {
        if (valorContado == null || valorContado.signum() < 0) {
            throw new RegraNegocioException("VALOR_CONTADO_INVALIDO", "Informe o valor contado em dinheiro (≥ 0).");
        }
        Caixa caixa = travarAberto(caixaId);
        List<Long> vendasAbertas = vendaRepository.idsPorCaixaEStatus(caixaId, StatusVenda.ABERTA);
        if (!vendasAbertas.isEmpty()) {
            throw new ConflitoException("CAIXA_COM_VENDA_ABERTA",
                    "Finalize ou cancele as vendas em aberto antes de fechar o caixa.",
                    Map.of("vendasAbertas", vendasAbertas));
        }
        caixa.fechar(saldoEsperado(caixa), Dinheiro.valor(valorContado), agora(), operadorId);
        log.info("Caixa #{} fechado: esperado {}, contado {}, diferença {} ({})", caixa.getId(),
                caixa.getSaldoEsperado(), caixa.getValorContado(), caixa.getDiferenca(), caixa.getSituacaoConferencia());
        return extrato(caixa);
    }

    @Transactional(readOnly = true)
    public ExtratoCaixa extrato(Long caixaId) {
        return extrato(buscar(caixaId));
    }

    private ExtratoCaixa extrato(Caixa caixa) {
        Long id = caixa.getId();
        Map<FormaPagamento, BigDecimal> porForma = new EnumMap<>(FormaPagamento.class);
        for (FormaPagamento forma : FormaPagamento.values()) {
            porForma.put(forma, Dinheiro.ZERO);
        }
        for (Object[] linha : vendaRepository.totaisPorForma(id, StatusVenda.FINALIZADA)) {
            porForma.put((FormaPagamento) linha[0], Dinheiro.valor((BigDecimal) linha[1]));
        }
        // No extrato, o dinheiro aparece líquido do troco devolvido.
        BigDecimal troco = vendaRepository.somaTroco(id, StatusVenda.FINALIZADA);
        porForma.put(FormaPagamento.DINHEIRO, Dinheiro.valor(porForma.get(FormaPagamento.DINHEIRO).subtract(troco)));

        long finalizadas = vendaRepository.countByCaixaIdAndStatus(id, StatusVenda.FINALIZADA);
        BigDecimal totalVendido = Dinheiro.valor(vendaRepository.somaTotal(id, StatusVenda.FINALIZADA));
        BigDecimal ticketMedio = finalizadas == 0 ? Dinheiro.ZERO
                : totalVendido.divide(BigDecimal.valueOf(finalizadas), 2, RoundingMode.HALF_EVEN);

        BigDecimal esperado = caixa.isAberto() ? saldoEsperado(caixa) : caixa.getSaldoEsperado();
        return new ExtratoCaixa(
                CaixaResponse.de(caixa, esperado),
                caixa.getSaldoInicial(),
                soma(id, TipoMovimentacaoCaixa.SUPRIMENTO),
                soma(id, TipoMovimentacaoCaixa.SANGRIA),
                soma(id, TipoMovimentacaoCaixa.VENDA_DINHEIRO),
                soma(id, TipoMovimentacaoCaixa.RECEBIMENTO_CLIENTE),
                soma(id, TipoMovimentacaoCaixa.ESTORNO_VENDA),
                esperado,
                caixa.getValorContado(),
                caixa.getDiferenca(),
                caixa.getSituacaoConferencia(),
                porForma,
                finalizadas,
                vendaRepository.countByCaixaIdAndStatus(id, StatusVenda.CANCELADA),
                vendaRepository.countByCaixaIdAndStatus(id, StatusVenda.ESTORNADA),
                totalVendido,
                ticketMedio,
                movimentacaoRepository.findByCaixaIdOrderByDataHoraAscIdAsc(id).stream()
                        .map(MovimentacaoCaixaResponse::de).toList());
    }

    private BigDecimal soma(Long caixaId, TipoMovimentacaoCaixa tipo) {
        return Dinheiro.valor(movimentacaoRepository.somaPorTipo(caixaId, tipo));
    }

    private Caixa travarAberto(Long caixaId) {
        Caixa caixa = repository.travarPorId(caixaId).orElseThrow(() -> new NaoEncontradoException("Caixa", caixaId));
        caixa.exigirAberto();
        return caixa;
    }

    private MovimentacaoCaixa registrar(Caixa caixa, TipoMovimentacaoCaixa tipo, BigDecimal valor, String descricao,
                                        Long vendaId, Long operadorId) {
        return movimentacaoRepository.save(
                new MovimentacaoCaixa(caixa, tipo, Dinheiro.valor(valor), descricao, vendaId, operadorId, agora()));
    }

    private static void exigirPositivo(BigDecimal valor) {
        if (!Dinheiro.positivo(valor)) {
            throw new RegraNegocioException("VALOR_INVALIDO", "O valor deve ser maior que zero.");
        }
    }

    private static String texto(String descricao, String padrao) {
        return StringUtils.hasText(descricao) ? descricao.trim() : padrao;
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(relogio);
    }
}
