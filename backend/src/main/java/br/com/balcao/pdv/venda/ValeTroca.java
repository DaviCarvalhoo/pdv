/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.comum.RegraNegocioException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/** Crédito para compras futuras, gerado numa troca. Usado como forma de pagamento {@code VALE_TROCA}. */
@Entity
@Table(name = "vale_troca")
@Getter
@NoArgsConstructor
public class ValeTroca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private BigDecimal valor;
    private BigDecimal saldo;
    private Long vendaOrigemId;
    private Long clienteId;
    private Long operadorId;
    private OffsetDateTime criadoEm;

    @Version
    private Long version;

    ValeTroca(String codigo, BigDecimal valor, Long vendaOrigemId, Long clienteId, Long operadorId,
              OffsetDateTime agora) {
        this.codigo = codigo;
        this.valor = valor;
        this.saldo = valor;
        this.vendaOrigemId = vendaOrigemId;
        this.clienteId = clienteId;
        this.operadorId = operadorId;
        this.criadoEm = agora;
    }

    void debitar(BigDecimal v) {
        if (v.compareTo(saldo) > 0) {
            throw new RegraNegocioException("VALE_SEM_SALDO",
                    "O vale-troca " + codigo + " tem " + saldo + " de saldo.", Map.of("saldo", saldo));
        }
        saldo = saldo.subtract(v);
    }

    void creditar(BigDecimal v) {
        saldo = saldo.add(v).min(valor);
    }
}
