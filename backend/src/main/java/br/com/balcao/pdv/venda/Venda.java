package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.caixa.Caixa;
import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.produto.Produto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agregado da venda. Todas as regras de carrinho, pagamento, troco e transição de status ficam aqui,
 * e os totais são sempre recalculados a partir dos itens e pagamentos.
 */
@Entity
@Table(name = "venda")
@Getter
@NoArgsConstructor
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Caixa caixa;

    @Enumerated(EnumType.STRING)
    private StatusVenda status;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemVenda> itens = new ArrayList<>();

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Pagamento> pagamentos = new ArrayList<>();

    private BigDecimal total = Dinheiro.ZERO;
    private BigDecimal valorPago = Dinheiro.ZERO;
    private BigDecimal troco = Dinheiro.ZERO;
    /** CPF ou CNPJ do consumidor para a NFC-e ("CPF na nota?"). */
    private String documentoConsumidor;

    private OffsetDateTime dataAbertura;
    private OffsetDateTime dataFinalizacao;
    private OffsetDateTime dataCancelamento;
    private String motivoCancelamento;

    @Version
    private Long version;

    public Venda(Caixa caixa, OffsetDateTime agora) {
        this.caixa = caixa;
        this.status = StatusVenda.ABERTA;
        this.dataAbertura = agora;
    }

    // ---------------------------------------------------------------- Itens

    public ItemVenda adicionarItem(Produto produto, BigDecimal quantidade) {
        exigirAberta();
        if (!produto.isAtivo()) {
            throw new RegraNegocioException("PRODUTO_INATIVO", "O produto " + produto.getNome() + " está inativo.");
        }
        validarQuantidade(produto, quantidade);
        // O mesmo produto passado de novo soma na linha existente (se o preço não mudou).
        ItemVenda item = itens.stream()
                .filter(i -> i.getProduto().getId().equals(produto.getId())
                        && i.getPrecoUnitario().compareTo(produto.getPreco()) == 0)
                .findFirst()
                .orElse(null);
        if (item == null) {
            item = new ItemVenda(this, produto, quantidade);
            itens.add(item);
        } else {
            item.alterarQuantidade(item.getQuantidade().add(quantidade));
        }
        recalcular();
        return item;
    }

    public void alterarQuantidade(Long itemId, BigDecimal quantidade) {
        exigirAberta();
        ItemVenda item = item(itemId);
        validarQuantidade(item.getProduto(), quantidade);
        BigDecimal anterior = item.getQuantidade();
        item.alterarQuantidade(quantidade);
        try {
            recalcular();
        } catch (RegraNegocioException e) {
            item.alterarQuantidade(anterior);
            recalcular();
            throw e;
        }
    }

    public void removerItem(Long itemId) {
        exigirAberta();
        ItemVenda item = item(itemId);
        int posicao = itens.indexOf(item);
        itens.remove(item);
        try {
            recalcular();
        } catch (RegraNegocioException e) {
            itens.add(posicao, item);
            recalcular();
            throw e;
        }
    }

    // ----------------------------------------------------------- Pagamentos

    public Pagamento adicionarPagamento(FormaPagamento forma, BigDecimal valor, String identificador,
                                        OffsetDateTime agora) {
        exigirAberta();
        if (itens.isEmpty() || total.signum() == 0) {
            throw new RegraNegocioException("VENDA_SEM_ITENS", "Adicione itens antes de lançar pagamentos.");
        }
        if (!Dinheiro.positivo(valor)) {
            throw new RegraNegocioException("VALOR_INVALIDO", "O valor do pagamento deve ser maior que zero.");
        }
        BigDecimal v = Dinheiro.valor(valor);
        BigDecimal restante = getRestante();
        if (restante.signum() == 0) {
            throw new RegraNegocioException("VENDA_JA_QUITADA", "A venda já está totalmente paga.");
        }
        if (forma != FormaPagamento.DINHEIRO && v.compareTo(restante) > 0) {
            throw new RegraNegocioException("PAGAMENTO_EXCEDE_RESTANTE",
                    "Pagamentos em " + forma + " não podem passar do valor restante (" + restante + "). "
                            + "Só o dinheiro gera troco.",
                    Map.of("restante", restante));
        }
        Pagamento pagamento = new Pagamento(this, forma, v, identificador, agora);
        pagamentos.add(pagamento);
        recalcular();
        return pagamento;
    }

    public void removerPagamento(Long pagamentoId) {
        exigirAberta();
        Pagamento pagamento = pagamentos.stream().filter(p -> Objects.equals(p.getId(), pagamentoId)).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Pagamento", pagamentoId));
        pagamentos.remove(pagamento);
        recalcular();
    }

    public void informarConsumidor(String documento) {
        exigirAberta();
        this.documentoConsumidor = documento;
    }

    // ------------------------------------------------------------- Status

    public void finalizar(OffsetDateTime agora) {
        exigirAberta();
        if (itens.isEmpty()) {
            throw new RegraNegocioException("VENDA_SEM_ITENS", "Não é possível finalizar uma venda sem itens.");
        }
        if (valorPago.compareTo(total) < 0) {
            throw new RegraNegocioException("PAGAMENTO_INSUFICIENTE",
                    "Falta receber " + getRestante() + " para finalizar a venda.",
                    Map.of("restante", getRestante()));
        }
        caixa.exigirAberto();
        this.status = StatusVenda.FINALIZADA;
        this.dataFinalizacao = agora;
    }

    public void cancelar(String motivo, OffsetDateTime agora) {
        if (status == StatusVenda.FINALIZADA) {
            throw new ConflitoException("VENDA_FINALIZADA",
                    "Venda finalizada não pode ser cancelada; use o estorno.");
        }
        exigirAberta();
        this.status = StatusVenda.CANCELADA;
        this.dataCancelamento = agora;
        this.motivoCancelamento = motivo;
    }

    public void estornar(String motivo, OffsetDateTime agora) {
        if (status != StatusVenda.FINALIZADA) {
            throw new ConflitoException("VENDA_NAO_FINALIZADA", "Só vendas finalizadas podem ser estornadas.");
        }
        if (!caixa.isAberto()) {
            throw new ConflitoException("CAIXA_FECHADO",
                    "Só é possível estornar vendas do caixa que ainda está aberto.");
        }
        this.status = StatusVenda.ESTORNADA;
        this.dataCancelamento = agora;
        this.motivoCancelamento = motivo;
    }

    // ------------------------------------------------------------- Cálculos

    public BigDecimal getRestante() {
        return total.subtract(valorPago).max(Dinheiro.ZERO);
    }

    public BigDecimal totalPorForma(FormaPagamento forma) {
        return Dinheiro.valor(pagamentos.stream().filter(p -> p.getForma() == forma)
                .map(Pagamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Dinheiro que efetivamente fica no caixa: recebido em dinheiro menos o troco. */
    public BigDecimal getDinheiroLiquido() {
        return totalPorForma(FormaPagamento.DINHEIRO).subtract(troco);
    }

    /**
     * Recalcula total, pago e troco. O troco sai só do dinheiro (RN-PAG-04). Se, depois de mexer nos itens,
     * PIX/cartão passarem do novo total, a alteração é recusada (RN-PAG-06).
     */
    private void recalcular() {
        this.total = Dinheiro.valor(itens.stream().map(ItemVenda::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        this.valorPago = Dinheiro.valor(pagamentos.stream().map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal naoDinheiro = valorPago.subtract(totalPorForma(FormaPagamento.DINHEIRO));
        if (naoDinheiro.compareTo(total) > 0) {
            throw new RegraNegocioException("PAGAMENTOS_EXCEDEM_TOTAL",
                    "Os pagamentos em PIX/cartão passam do novo total. Remova um pagamento antes.");
        }
        this.troco = valorPago.subtract(total).max(Dinheiro.ZERO);
    }

    private void exigirAberta() {
        if (status != StatusVenda.ABERTA) {
            throw new ConflitoException("VENDA_NAO_ABERTA",
                    "A venda #" + id + " está " + status + " e não pode ser alterada.");
        }
    }

    private ItemVenda item(Long itemId) {
        return itens.stream().filter(i -> Objects.equals(i.getId(), itemId)).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Item", itemId));
    }

    private static void validarQuantidade(Produto produto, BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new RegraNegocioException("QUANTIDADE_INVALIDA", "A quantidade deve ser maior que zero.");
        }
        if ("UN".equals(produto.getUnidade()) && quantidade.stripTrailingZeros().scale() > 0) {
            throw new RegraNegocioException("QUANTIDADE_FRACIONADA",
                    produto.getNome() + " é vendido por unidade; use uma quantidade inteira.");
        }
    }
}
