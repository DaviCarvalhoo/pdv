package br.com.balcao.pdv.loja;

import br.com.balcao.pdv.estoque.PoliticaSaldoInsuficiente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Identidade visual e regras da venda (registro único, id = 1). */
@Entity
@Table(name = "loja")
@Getter
@Setter
@NoArgsConstructor
public class Loja {

    public static final long ID = 1L;

    @Id
    private Long id;

    private String nomeFantasia;
    private String slogan;
    /** Data URL (data:image/png;base64,...) — cabe no banco e dispensa servidor de arquivos. */
    private String logo;
    private String corDestaque;
    private String mensagemCupom;

    @Enumerated(EnumType.STRING)
    private PoliticaSaldoInsuficiente politicaEstoque;
    /** Acima disso o PDV sugere sangria. Nulo = sem alerta. */
    private BigDecimal limiteGaveta;
    /** Desconto (%) que o operador dá sem autorização do gerente. */
    private BigDecimal descontoMaxOperador;

    private String balancaPrefixo;
    private Integer balancaDigitosCodigo;

    @Enumerated(EnumType.STRING)
    private TipoValorBalanca balancaTipoValor;

    // PIX estático (BR Code) gerado no valor exato da venda
    private String chavePix;
    private String pixRecebedor;
    private String pixCidade;

    /** Alíquota aproximada padrão (Lei 12.741) quando o produto não tem a sua. */
    private BigDecimal aliquotaTributos;

    private OffsetDateTime atualizadoEm;

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = OffsetDateTime.now();
    }
}
