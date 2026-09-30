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
