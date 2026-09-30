package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.caixa.Caixa;
import br.com.balcao.pdv.cliente.Cliente;
import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agregado da venda. Todas as regras de carrinho, desconto, pagamento, troco e transição de status ficam aqui,
 * e os totais são sempre recalculados a partir dos itens e pagamentos.
 */
@Entity
@Table(name = "venda")
@Getter
@NoArgsConstructor
public class Venda {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Caixa caixa;

    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario operador;

    @ManyToOne(fetch = FetchType.LAZY)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    private StatusVenda status;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemVenda> itens = new ArrayList<>();

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Pagamento> pagamentos = new ArrayList<>();

    /** Soma dos itens, antes do desconto. */
    private BigDecimal subtotal = Dinheiro.ZERO;
    private BigDecimal desconto = Dinheiro.ZERO;
    /** Quando o desconto foi dado em %, é reaplicado se os itens mudarem. */
    private BigDecimal descontoPercentual;
    /** Subtotal − desconto. */
    private BigDecimal total = Dinheiro.ZERO;
    private BigDecimal valorPago = Dinheiro.ZERO;
    private BigDecimal troco = Dinheiro.ZERO;
    /** Tributos aproximados (Lei 12.741), calculados na finalização. */
    private BigDecimal tributosAprox = Dinheiro.ZERO;
    /** CPF ou CNPJ do consumidor para a NFC-e ("CPF na nota?"). */
    private String documentoConsumidor;

    /** Venda estacionada para atender outro cliente. */
    private boolean emEspera;
    /** Apelido da venda em espera ("moça do boné", "mesa 3"). */
    private String identificacao;

    private OffsetDateTime dataAbertura;
    private OffsetDateTime dataFinalizacao;
    private OffsetDateTime dataCancelamento;
    private String motivoCancelamento;

    @Version
    private Long version;

    public Venda(Caixa caixa, OffsetDateTime agora) {
        this(caixa, null, agora);
    }

    public Venda(Caixa caixa, Usuario operador, OffsetDateTime agora) {
        this.caixa = caixa;
        this.operador = operador;
        this.status = StatusVenda.ABERTA;
        this.dataAbertura = agora;
    }

    // ---------------------------------------------------------------- Itens

    public ItemVenda adicionarItem(Produto produto, BigDecimal quantidade) {
        return adicionarItem(produto, quantidade, LocalDate.now());
    }

    public ItemVenda adicionarItem(Produto produto, BigDecimal quantidade, LocalDate dia) {
        exigirAberta();
        if (!produto.isAtivo()) {
            throw new RegraNegocioException("PRODUTO_INATIVO", "O produto " + produto.getNome() + " está inativo.");
        }
        validarQuantidade(produto, quantidade);
        BigDecimal preco = produto.precoVigente(dia);
        // O mesmo produto passado de novo soma na linha existente (se o preço não mudou).
        ItemVenda item = itens.stream()
                .filter(i -> i.getProduto().getId().equals(produto.getId())
                        && i.getPrecoUnitario().compareTo(preco) == 0)
                .findFirst()
                .orElse(null);
        if (item == null) {
            item = new ItemVenda(this, produto, quantidade, dia);
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

    // ------------------------------------------------------------ Desconto

    /**
     * Desconto no total, em valor ou em percentual (um dos dois). Zero/nulo nos dois remove o desconto.
     *
     * @return percentual efetivo, para a checagem do limite do operador
     */
    public BigDecimal aplicarDesconto(BigDecimal valor, BigDecimal percentual) {
        exigirAberta();
        if (subtotal.signum() == 0) {
            throw new RegraNegocioException("VENDA_SEM_ITENS", "Adicione itens antes de dar desconto.");
        }
        BigDecimal anteriorValor = desconto;
        BigDecimal anteriorPct = descontoPercentual;
        if (percentual != null && percentual.signum() > 0) {
            if (percentual.compareTo(CEM) >= 0) {
                throw new RegraNegocioException("DESCONTO_INVALIDO", "O desconto deve ser menor que 100%.");
            }
            descontoPercentual = percentual.setScale(2, RoundingMode.HALF_EVEN);
            desconto = Dinheiro.ZERO;
        } else if (valor != null && valor.signum() > 0) {
            if (Dinheiro.valor(valor).compareTo(subtotal) >= 0) {
                throw new RegraNegocioException("DESCONTO_INVALIDO", "O desconto deve ser menor que o subtotal.");
            }
            descontoPercentual = null;
            desconto = Dinheiro.valor(valor);
        } else {
            descontoPercentual = null;
            desconto = Dinheiro.ZERO;
        }
        try {
            recalcular();
        } catch (RegraNegocioException e) {
            desconto = anteriorValor;
            descontoPercentual = anteriorPct;
            recalcular();
            throw e;
        }
        return getPercentualDesconto();
    }

    public BigDecimal getPercentualDesconto() {
        return subtotal.signum() == 0 ? BigDecimal.ZERO
                : desconto.multiply(CEM).divide(subtotal, 2, RoundingMode.HALF_EVEN);
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
        if (forma == FormaPagamento.CREDIARIO && cliente == null) {
            throw new RegraNegocioException("VENDA_SEM_CLIENTE", "Para vender fiado, identifique o cliente (F5).");
        }
        BigDecimal v = Dinheiro.valor(valor);
        BigDecimal restante = getRestante();
        if (restante.signum() == 0) {
            throw new RegraNegocioException("VENDA_JA_QUITADA", "A venda já está totalmente paga.");
        }
        if (forma != FormaPagamento.DINHEIRO && v.compareTo(restante) > 0) {
            throw new RegraNegocioException("PAGAMENTO_EXCEDE_RESTANTE",
                    "Pagamentos em " + forma.rotulo() + " não podem passar do valor restante (" + restante + "). "
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

    /** Vincula (ou remove, com nulo) o cliente. O CPF/CNPJ dele vai para a nota. */
    public void vincularCliente(Cliente novo) {
        exigirAberta();
        if (novo == null && totalPorForma(FormaPagamento.CREDIARIO).signum() > 0) {
            throw new RegraNegocioException("VENDA_COM_FIADO", "Remova o pagamento fiado antes de tirar o cliente.");
        }
        if (novo != null && !novo.isAtivo()) {
            throw new RegraNegocioException("CLIENTE_INATIVO", "O cliente " + novo.getNome() + " está inativo.");
        }
        this.cliente = novo;
        if (novo != null && novo.getDocumento() != null) {
            this.documentoConsumidor = novo.getDocumento();
        }
    }

    // ------------------------------------------------------------- Espera

    public void colocarEmEspera(String identificacao) {
        exigirAberta();
        if (itens.isEmpty()) {
            throw new RegraNegocioException("VENDA_SEM_ITENS", "Não há o que deixar em espera.");
        }
        this.emEspera = true;
        this.identificacao = identificacao;
    }

    public void retomar() {
        exigirAberta();
        this.emEspera = false;
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
        this.emEspera = false;
        this.dataFinalizacao = agora;
    }

    public void registrarTributos(BigDecimal valor) {
        this.tributosAprox = Dinheiro.valor(valor);
    }

    public void cancelar(String motivo, OffsetDateTime agora) {
        if (status == StatusVenda.FINALIZADA) {
            throw new ConflitoException("VENDA_FINALIZADA",
                    "Venda finalizada não pode ser cancelada; use o estorno.");
        }
        exigirAberta();
        this.status = StatusVenda.CANCELADA;
        this.emEspera = false;
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
     * Recalcula subtotal, desconto, total, pago e troco. O troco sai só do dinheiro (RN-PAG-04). Se, depois de
     * mexer nos itens ou no desconto, os pagamentos que não são dinheiro passarem do novo total, a alteração é
     * recusada (RN-PAG-06).
     */
    private void recalcular() {
        this.subtotal = Dinheiro.valor(itens.stream().map(ItemVenda::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (descontoPercentual != null) {
            this.desconto = Dinheiro.valor(subtotal.multiply(descontoPercentual).divide(CEM, 2, RoundingMode.HALF_EVEN));
        }
        if (desconto.compareTo(subtotal) >= 0) {
            // Itens removidos deixaram o desconto maior que a venda: some o desconto.
            this.desconto = Dinheiro.ZERO;
            this.descontoPercentual = null;
        }
        this.total = subtotal.subtract(desconto);
        this.valorPago = Dinheiro.valor(pagamentos.stream().map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal naoDinheiro = valorPago.subtract(totalPorForma(FormaPagamento.DINHEIRO));
        if (naoDinheiro.compareTo(total) > 0) {
            throw new RegraNegocioException("PAGAMENTOS_EXCEDEM_TOTAL",
                    "Os pagamentos que não são em dinheiro passam do novo total. Remova um pagamento antes.");
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
