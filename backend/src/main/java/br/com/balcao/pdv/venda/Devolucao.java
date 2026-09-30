package br.com.balcao.pdv.venda;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Registro de uma troca ou devolução parcial de uma venda finalizada. */
@Entity
@Table(name = "devolucao")
@Getter
@NoArgsConstructor
public class Devolucao {

    public enum Destino {
        /** Dinheiro devolvido da gaveta. */
        DINHEIRO,
        /** Crédito em vale-troca para uma compra futura. */
        VALE_TROCA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vendaId;
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private Destino destino;

    @ManyToOne(fetch = FetchType.EAGER)
    private ValeTroca valeTroca;

    private String motivo;
    private Long operadorId;
    private OffsetDateTime dataHora;

    Devolucao(Long vendaId, BigDecimal valor, Destino destino, ValeTroca vale, String motivo, Long operadorId,
              OffsetDateTime agora) {
        this.vendaId = vendaId;
        this.valor = valor;
        this.destino = destino;
        this.valeTroca = vale;
        this.motivo = motivo;
        this.operadorId = operadorId;
        this.dataHora = agora;
    }
}
