package br.com.balcao.pdv.caixa;

import br.com.balcao.pdv.comum.ConflitoException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "caixa")
@Getter
@NoArgsConstructor
public class Caixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private StatusCaixa status;

    private BigDecimal saldoInicial;
    private OffsetDateTime dataAbertura;
    private OffsetDateTime dataFechamento;

    // Preenchidos no fechamento (conferência)
    private BigDecimal saldoEsperado;
    private BigDecimal valorContado;
    private BigDecimal diferenca;

    @Enumerated(EnumType.STRING)
    private SituacaoConferencia situacaoConferencia;

    private Long operadorAberturaId;
    private Long operadorFechamentoId;

    @Version
    private Long version;

    public Caixa(BigDecimal saldoInicial, OffsetDateTime agora) {
        this(saldoInicial, agora, null);
    }

    public Caixa(BigDecimal saldoInicial, OffsetDateTime agora, Long operadorId) {
        this.operadorAberturaId = operadorId;
        this.status = StatusCaixa.ABERTO;
        this.saldoInicial = saldoInicial;
        this.dataAbertura = agora;
    }

    public boolean isAberto() {
        return status == StatusCaixa.ABERTO;
    }

    public void exigirAberto() {
        if (!isAberto()) {
            throw new ConflitoException("CAIXA_FECHADO", "O caixa #" + id + " está fechado.");
        }
    }

    void fechar(BigDecimal esperado, BigDecimal contado, OffsetDateTime agora, Long operadorId) {
        exigirAberto();
        this.operadorFechamentoId = operadorId;
        this.saldoEsperado = esperado;
        this.valorContado = contado;
        this.diferenca = contado.subtract(esperado);
        this.situacaoConferencia = SituacaoConferencia.de(diferenca);
        this.dataFechamento = agora;
        this.status = StatusCaixa.FECHADO;
    }
}
