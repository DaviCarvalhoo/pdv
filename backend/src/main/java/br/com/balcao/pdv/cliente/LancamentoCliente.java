/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.cliente;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Linha da conta corrente (fiado) do cliente. */
@Entity
@Table(name = "lancamento_cliente")
@Getter
@NoArgsConstructor
public class LancamentoCliente {

    public enum Tipo {
        /** Compra no crediário: aumenta a dívida. */
        COMPRA,
        /** Pagamento recebido: diminui a dívida. */
        PAGAMENTO,
        /** Estorno de uma compra: diminui a dívida. */
        ESTORNO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    private BigDecimal valor;
    private String forma;
    private Long vendaId;
    private Long operadorId;
    private String observacao;
    private BigDecimal saldoApos;
    private OffsetDateTime dataHora;

    LancamentoCliente(Cliente cliente, Tipo tipo, BigDecimal valor, String forma, Long vendaId, Long operadorId,
                      String observacao, OffsetDateTime agora) {
        this.cliente = cliente;
        this.tipo = tipo;
        this.valor = valor;
        this.forma = forma;
        this.vendaId = vendaId;
        this.operadorId = operadorId;
        this.observacao = observacao;
        this.saldoApos = cliente.getSaldoDevedor();
        this.dataHora = agora;
    }
}
