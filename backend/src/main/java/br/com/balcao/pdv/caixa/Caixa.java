/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

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
