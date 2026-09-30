package br.com.balcao.pdv.cliente;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    /** CPF ou CNPJ, só dígitos. */
    private String documento;
    private String telefone;
    private String email;
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    /** Só muda por lançamento na conta (ver ClienteService). */
    @Setter(AccessLevel.NONE)
    private BigDecimal saldoDevedor = BigDecimal.ZERO;

    private String observacao;
    private boolean ativo = true;
    private OffsetDateTime criadoEm;

    @Version
    private Long version;

    public BigDecimal getCreditoDisponivel() {
        return limiteCredito.subtract(saldoDevedor).max(BigDecimal.ZERO);
    }

    void aplicarSaldo(BigDecimal saldo) {
        this.saldoDevedor = saldo;
    }

    @PrePersist
    void aoCriar() {
        criadoEm = OffsetDateTime.now();
    }
}
