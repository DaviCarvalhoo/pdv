/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

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
    private Long operadorId;
    private OffsetDateTime dataHora;

    public MovimentacaoCaixa(Caixa caixa, TipoMovimentacaoCaixa tipo, BigDecimal valor, String descricao,
                             Long vendaId, Long operadorId, OffsetDateTime agora) {
        this.operadorId = operadorId;
        this.caixa = caixa;
        this.tipo = tipo;
        this.valor = valor;
        this.descricao = descricao;
        this.vendaId = vendaId;
        this.dataHora = agora;
    }
}
