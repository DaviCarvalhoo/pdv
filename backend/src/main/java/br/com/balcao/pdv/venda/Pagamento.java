package br.com.balcao.pdv.venda;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pagamento")
@Getter
@NoArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Venda venda;

    @Enumerated(EnumType.STRING)
    private FormaPagamento forma;

    private BigDecimal valor;
    /** NSU/autorização do cartão ou E2E ID do PIX (opcional). */
    private String identificadorTransacao;
    private OffsetDateTime dataHora;

    Pagamento(Venda venda, FormaPagamento forma, BigDecimal valor, String identificadorTransacao,
              OffsetDateTime agora) {
        this.venda = venda;
        this.forma = forma;
        this.valor = valor;
        this.identificadorTransacao = identificadorTransacao;
        this.dataHora = agora;
    }
}
