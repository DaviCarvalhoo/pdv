package br.com.balcao.pdv.caixa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "movimentacao_caixa")
@Getter
@NoArgsConstructor
public class MovimentacaoCaixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Caixa caixa;

    @Enumerated(EnumType.STRING)
    private TipoMovimentacaoCaixa tipo;

    /** Sempre positivo; o sentido vem do tipo. */
    private BigDecimal valor;
    private String descricao;
    private Long vendaId;
    private OffsetDateTime dataHora;

    public MovimentacaoCaixa(Caixa caixa, TipoMovimentacaoCaixa tipo, BigDecimal valor, String descricao,
                             Long vendaId, OffsetDateTime agora) {
        this.caixa = caixa;
        this.tipo = tipo;
        this.valor = valor;
        this.descricao = descricao;
        this.vendaId = vendaId;
        this.dataHora = agora;
    }
}
