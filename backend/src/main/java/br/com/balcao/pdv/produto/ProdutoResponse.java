package br.com.balcao.pdv.produto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProdutoResponse(
        Long id, String codigoInterno, String gtin, String nome, BigDecimal preco, String unidade,
        String ncm, String cfop, Integer origem, String csosn,
        BigDecimal estoqueAtual, BigDecimal estoqueMinimo, boolean estoqueBaixo,
        boolean ativo, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {

    public static ProdutoResponse de(Produto p) {
        return new ProdutoResponse(p.getId(), p.getCodigoInterno(), p.getGtin(), p.getNome(), p.getPreco(),
                p.getUnidade(), p.getNcm(), p.getCfop(), p.getOrigem(), p.getCsosn(),
                p.getEstoqueAtual(), p.getEstoqueMinimo(), p.isEstoqueBaixo(),
                p.isAtivo(), p.getCriadoEm(), p.getAtualizadoEm());
    }
}
