/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

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
