package br.com.balcao.pdv.produto;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "produto")
@Getter
@Setter
@NoArgsConstructor
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigoInterno;
    private String gtin;
    private String nome;
    private BigDecimal preco;
    private String unidade = "UN";

    // Dados fiscais (NFC-e)
    private String ncm;
    private String cfop = "5102";
    private Integer origem = 0;
    private String csosn = "102";

    /** Só muda por meio de uma movimentação de estoque (ver EstoqueService). */
    @Setter(AccessLevel.NONE)
    private BigDecimal estoqueAtual = BigDecimal.ZERO;
    private BigDecimal estoqueMinimo;

    private boolean ativo = true;
    private OffsetDateTime criadoEm;
    private OffsetDateTime atualizadoEm;

    @Version
    private Long version;

    public boolean isEstoqueBaixo() {
        return estoqueMinimo != null && estoqueAtual.compareTo(estoqueMinimo) <= 0;
    }

    /** Chamado apenas pelo EstoqueService, junto com o registro da movimentação. */
    public void aplicarSaldoEstoque(BigDecimal novoSaldo) {
        this.estoqueAtual = novoSaldo;
    }

    @PrePersist
    void aoCriar() {
        criadoEm = OffsetDateTime.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = OffsetDateTime.now();
    }
}
