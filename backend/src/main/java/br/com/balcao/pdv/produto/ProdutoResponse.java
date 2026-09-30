package br.com.balcao.pdv.produto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ProdutoResponse(
        Long id, String codigoInterno, String gtin, String nome, BigDecimal preco, String unidade,
        String ncm, String cfop, Integer origem, String csosn,
        BigDecimal estoqueAtual, BigDecimal estoqueMinimo, boolean estoqueBaixo,
        boolean ativo, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm,
        Long categoriaId, String categoriaNome, String categoriaCor,
        BigDecimal precoCusto, BigDecimal margem,
        BigDecimal precoPromocional, LocalDate promocaoInicio, LocalDate promocaoFim,
        boolean emPromocao, BigDecimal precoVigente,
        boolean atalhoRapido, BigDecimal aliquotaTributos) {

    public static ProdutoResponse de(Produto p) {
        LocalDate hoje = LocalDate.now();
        Categoria c = p.getCategoria();
        return new ProdutoResponse(p.getId(), p.getCodigoInterno(), p.getGtin(), p.getNome(), p.getPreco(),
                p.getUnidade(), p.getNcm(), p.getCfop(), p.getOrigem(), p.getCsosn(),
                p.getEstoqueAtual(), p.getEstoqueMinimo(), p.isEstoqueBaixo(),
                p.isAtivo(), p.getCriadoEm(), p.getAtualizadoEm(),
                c != null ? c.getId() : null, c != null ? c.getNome() : null, c != null ? c.getCor() : null,
                p.getPrecoCusto(), p.margem(),
                p.getPrecoPromocional(), p.getPromocaoInicio(), p.getPromocaoFim(),
                p.emPromocao(hoje), p.precoVigente(hoje),
                p.isAtalhoRapido(), p.getAliquotaTributos());
    }
}
