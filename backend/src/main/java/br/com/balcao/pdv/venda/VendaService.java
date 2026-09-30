package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.caixa.Caixa;
import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.comum.Documento;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.estoque.EstoqueService;
import br.com.balcao.pdv.fiscal.NotaFiscalRepository;
import br.com.balcao.pdv.fiscal.NotaFiscalResumo;
import br.com.balcao.pdv.fiscal.StatusNota;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.produto.ProdutoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendaService {

    private final VendaRepository repository;
    private final CaixaService caixaService;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;
    private final NotaFiscalRepository notaRepository;
    private final EntityManager entityManager;
    private final Clock relogio;

    public record Totalizadores(long quantidade, BigDecimal valorTotal) {
    }

    @Transactional
    public VendaResponse iniciar() {
        Caixa caixa = caixaService.exigirAberto();
        repository.findFirstByCaixaIdAndStatusOrderByIdDesc(caixa.getId(), StatusVenda.ABERTA).ifPresent(v -> {
            throw new ConflitoException("VENDA_EM_ANDAMENTO",
                    "Já existe uma venda em andamento (#" + v.getId() + ").", Map.of("vendaId", v.getId()));
        });
        return resposta(repository.save(new Venda(caixa, agora())));
    }

    @Transactional(readOnly = true)
    public Optional<VendaResponse> emAndamento() {
        return caixaService.aberto()
                .flatMap(c -> repository.findFirstByCaixaIdAndStatusOrderByIdDesc(c.getId(), StatusVenda.ABERTA))
                .map(this::resposta);
    }

    @Transactional(readOnly = true)
    public VendaResponse detalhar(Long id) {
        return resposta(buscar(id));
    }

    @Transactional
    public VendaResponse adicionarItem(Long vendaId, Long produtoId, String codigo, BigDecimal quantidade) {
        Venda venda = buscar(vendaId);
        Produto produto;
        if (produtoId != null) {
            produto = produtoService.buscar(produtoId);
        } else if (StringUtils.hasText(codigo)) {
            produto = produtoService.porCodigo(codigo.trim());
        } else {
            throw new RegraNegocioException("PRODUTO_NAO_INFORMADO", "Informe o produto ou o código.");
        }
        venda.adicionarItem(produto, quantidade != null ? quantidade : BigDecimal.ONE);
        return salvar(venda);
    }

    @Transactional
    public VendaResponse alterarQuantidade(Long vendaId, Long itemId, BigDecimal quantidade) {
        Venda venda = buscar(vendaId);
        venda.alterarQuantidade(itemId, quantidade);
        return salvar(venda);
    }

    @Transactional
    public VendaResponse removerItem(Long vendaId, Long itemId) {
        Venda venda = buscar(vendaId);
        venda.removerItem(itemId);
        return salvar(venda);
    }

    @Transactional
    public VendaResponse adicionarPagamento(Long vendaId, FormaPagamento forma, BigDecimal valor,
                                            String identificador) {
        Venda venda = buscar(vendaId);
        venda.adicionarPagamento(forma, valor, StringUtils.hasText(identificador) ? identificador.trim() : null,
                agora());
        return salvar(venda);
    }

    @Transactional
    public VendaResponse removerPagamento(Long vendaId, Long pagamentoId) {
        Venda venda = buscar(vendaId);
        venda.removerPagamento(pagamentoId);
        return salvar(venda);
    }

    @Transactional
    public VendaResponse informarConsumidor(Long vendaId, String documento) {
        Venda venda = buscar(vendaId);
        String digitos = Documento.somenteDigitos(documento);
        if (!digitos.isEmpty() && !Documento.valido(digitos)) {
            throw new RegraNegocioException("DOCUMENTO_INVALIDO", "CPF/CNPJ inválido.");
        }
        venda.informarConsumidor(digitos.isEmpty() ? null : digitos);
        return salvar(venda);
    }

    /**
     * Finaliza numa única transação (RN-VEN-04): status, baixa de estoque e entrada do dinheiro no caixa.
     * O caixa fica travado até o commit, então não pode ser fechado no meio da finalização.
     */
    @Transactional
    public void finalizar(Long vendaId) {
        Venda venda = buscar(vendaId);
        caixaService.travar(venda.getCaixa().getId());
        venda.finalizar(agora());
        for (ItemVenda item : venda.getItens()) {
            estoqueService.saidaVenda(item.getProduto().getId(), item.getQuantidade(), venda.getId());
        }
        caixaService.registrarVendaDinheiro(venda.getCaixa(), venda.getDinheiroLiquido(), venda.getId());
        repository.saveAndFlush(venda);
        log.info("Venda #{} finalizada: total {}, pago {}, troco {}", venda.getId(), venda.getTotal(),
                venda.getValorPago(), venda.getTroco());
    }

    @Transactional
    public VendaResponse cancelar(Long vendaId, String motivo) {
        Venda venda = buscar(vendaId);
        venda.cancelar(StringUtils.hasText(motivo) ? motivo.trim() : null, agora());
        log.info("Venda #{} cancelada", vendaId);
        return salvar(venda);
    }

    @Transactional
    public VendaResponse estornar(Long vendaId, String motivo) {
        Venda venda = buscar(vendaId);
        if (notaRepository.existsByVendaIdAndStatus(vendaId, StatusNota.AUTORIZADA)) {
            throw new ConflitoException("NOTA_AUTORIZADA",
                    "Cancele a NFC-e autorizada desta venda antes de estornar.");
        }
        Caixa caixa = caixaService.travar(venda.getCaixa().getId());
        venda.estornar(StringUtils.hasText(motivo) ? motivo.trim() : null, agora());
        for (ItemVenda item : venda.getItens()) {
            estoqueService.estornoVenda(item.getProduto().getId(), item.getQuantidade(), venda.getId());
        }
        caixaService.registrarEstornoVenda(caixa, venda.getDinheiroLiquido(), venda.getId());
        log.info("Venda #{} estornada", vendaId);
        return salvar(venda);
    }

    @Transactional(readOnly = true)
    public Page<VendaResumo> historico(FiltroVendas filtro, Pageable pageable) {
        return repository.findAll(filtro.especificacao(), pageable).map(VendaResumo::de);
    }

    @Transactional(readOnly = true)
    public Totalizadores totalizadores(FiltroVendas filtro) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createTupleQuery();
        var root = query.from(Venda.class);
        query.multiselect(cb.count(root), cb.coalesce(cb.sum(root.<BigDecimal>get("total")), BigDecimal.ZERO))
                .where(filtro.especificacao().toPredicate(root, query, cb));
        Tuple t = entityManager.createQuery(query).getSingleResult();
        return new Totalizadores(t.get(0, Long.class), Dinheiro.valor(t.get(1, BigDecimal.class)));
    }

    private Venda buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NaoEncontradoException("Venda", id));
    }

    private VendaResponse salvar(Venda venda) {
        return resposta(repository.saveAndFlush(venda));
    }

    private VendaResponse resposta(Venda venda) {
        NotaFiscalResumo nota = notaRepository.findFirstByVendaIdOrderByIdDesc(venda.getId())
                .map(NotaFiscalResumo::de).orElse(null);
        return VendaResponse.de(venda, nota, avisosEstoque(venda));
    }

    private List<String> avisosEstoque(Venda venda) {
        List<String> avisos = new ArrayList<>();
        if (venda.getStatus() == StatusVenda.ABERTA) {
            for (ItemVenda item : venda.getItens()) {
                BigDecimal disponivel = item.getProduto().getEstoqueAtual();
                if (disponivel.compareTo(item.getQuantidade()) < 0) {
                    avisos.add("Estoque de " + item.getDescricao() + ": " + disponivel.stripTrailingZeros()
                            .toPlainString() + " disponível, " + item.getQuantidade().stripTrailingZeros()
                            .toPlainString() + " na venda.");
                }
            }
        }
        return avisos;
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(relogio);
    }
}
