package br.com.balcao.pdv.fiscal;

import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.venda.FormaPagamento;
import br.com.balcao.pdv.venda.Venda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Tudo o que o frontend precisa para imprimir o DANFE NFC-e (cupom 80 mm). */
public record DanfeNfce(
        Emitente emitente,
        Long notaId, Long vendaId, Integer serie, Integer numero, StatusNota status, Ambiente ambiente,
        OffsetDateTime dataEmissao, String chaveAcesso, String chaveFormatada, String protocolo,
        OffsetDateTime dataAutorizacao, String consumidor,
        List<Item> itens, int quantidadeItens, BigDecimal valorTotal,
        List<PagamentoDanfe> pagamentos, BigDecimal troco,
        String urlConsulta, String urlQrCode) {

    public record Emitente(String razaoSocial, String nomeFantasia, String cnpj, String inscricaoEstadual,
                           String endereco) {
    }

    public record Item(int numero, String codigo, String descricao, BigDecimal quantidade, String unidade,
                       BigDecimal valorUnitario, BigDecimal valorTotal) {
    }

    public record PagamentoDanfe(FormaPagamento forma, BigDecimal valor) {
    }

    static DanfeNfce de(NotaFiscal n, ConfiguracaoFiscal c) {
        Venda v = n.getVenda();
        String endereco = String.join(", ", c.getLogradouro(), c.getNumero(), c.getBairro())
                + " - " + c.getMunicipio() + "/" + c.getUf();
        var itens = new java.util.ArrayList<Item>();
        int i = 1;
        for (var item : v.getItens()) {
            Produto p = item.getProduto();
            String codigo = p.getGtin() != null ? p.getGtin()
                    : p.getCodigoInterno() != null ? p.getCodigoInterno() : String.valueOf(p.getId());
            itens.add(new Item(i++, codigo, item.getDescricao(), item.getQuantidade(), p.getUnidade(),
                    item.getPrecoUnitario(), item.getSubtotal()));
        }
        return new DanfeNfce(
                new Emitente(c.getRazaoSocial(), c.getNomeFantasia(), c.getCnpj(), c.getInscricaoEstadual(), endereco),
                n.getId(), v.getId(), n.getSerie(), n.getNumero(), n.getStatus(), n.getAmbiente(), n.getDataEmissao(),
                n.getChaveAcesso(), ChaveAcesso.formatada(n.getChaveAcesso()), n.getProtocolo(),
                n.getDataAutorizacao(), v.getDocumentoConsumidor(), itens, itens.size(), v.getTotal(),
                v.getPagamentos().stream().map(p -> new PagamentoDanfe(p.getForma(), p.getValor())).toList(),
                v.getTroco(), c.getUrlConsulta(), n.getUrlQrCode());
    }
}
