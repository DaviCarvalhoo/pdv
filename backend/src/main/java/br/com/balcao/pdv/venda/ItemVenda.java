package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.produto.Produto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "item_venda")
@Getter
@NoArgsConstructor
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Produto produto;

    /** Nome do produto no momento da venda. */
    private String descricao;
    /** Preço praticado no momento da venda — não muda se o cadastro mudar depois. */
    private BigDecimal precoUnitario;
    private BigDecimal quantidade;
    private BigDecimal subtotal;

    ItemVenda(Venda venda, Produto produto, BigDecimal quantidade) {
        this.venda = venda;
        this.produto = produto;
        this.descricao = produto.getNome();
        this.precoUnitario = produto.getPreco();
        alterarQuantidade(quantidade);
    }

    void alterarQuantidade(BigDecimal quantidade) {
        this.quantidade = Dinheiro.quantidade(quantidade);
        this.subtotal = Dinheiro.valor(precoUnitario.multiply(this.quantidade));
    }
}
