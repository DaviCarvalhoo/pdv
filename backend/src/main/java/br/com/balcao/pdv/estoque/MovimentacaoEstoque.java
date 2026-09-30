package br.com.balcao.pdv.estoque;

import br.com.balcao.pdv.produto.Produto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "movimentacao_estoque")
@Getter
@NoArgsConstructor
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Produto produto;

    @Enumerated(EnumType.STRING)
    private TipoMovimentacaoEstoque tipo;

    /** Quantidade com sinal: positiva entra, negativa sai. */
    private BigDecimal quantidade;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoPosterior;
    private Long vendaId;
    private String observacao;
    private OffsetDateTime dataHora;

    public MovimentacaoEstoque(Produto produto, TipoMovimentacaoEstoque tipo, BigDecimal quantidade,
                               BigDecimal saldoAnterior, Long vendaId, String observacao, OffsetDateTime agora) {
        this.produto = produto;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.saldoAnterior = saldoAnterior;
        this.saldoPosterior = saldoAnterior.add(quantidade);
        this.vendaId = vendaId;
        this.observacao = observacao;
        this.dataHora = agora;
    }
}
