/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

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
