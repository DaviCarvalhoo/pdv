package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.fiscal.NotaFiscalResumo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record VendaResponse(
        Long id, Long caixaId, StatusVenda status,
        List<Item> itens, List<PagamentoResponse> pagamentos,
        int quantidadeItens, BigDecimal total, BigDecimal valorPago, BigDecimal restante, BigDecimal troco,
        String documentoConsumidor,
        OffsetDateTime dataAbertura, OffsetDateTime dataFinalizacao, OffsetDateTime dataCancelamento,
        String motivoCancelamento,
        NotaFiscalResumo notaFiscal,
        List<String> avisos) {

    public record Item(Long id, Long produtoId, String codigo, String descricao, String unidade,
                       BigDecimal precoUnitario, BigDecimal quantidade, BigDecimal subtotal,
                       BigDecimal estoqueDisponivel) {
        static Item de(ItemVenda i) {
            var p = i.getProduto();
            String codigo = p.getGtin() != null ? p.getGtin() : p.getCodigoInterno();
            return new Item(i.getId(), p.getId(), codigo, i.getDescricao(), p.getUnidade(), i.getPrecoUnitario(),
                    i.getQuantidade(), i.getSubtotal(), p.getEstoqueAtual());
        }
    }

    public record PagamentoResponse(Long id, FormaPagamento forma, BigDecimal valor, String identificadorTransacao,
                                    OffsetDateTime dataHora) {
        static PagamentoResponse de(Pagamento p) {
            return new PagamentoResponse(p.getId(), p.getForma(), p.getValor(), p.getIdentificadorTransacao(),
                    p.getDataHora());
        }
    }

    public static VendaResponse de(Venda v, NotaFiscalResumo nota, List<String> avisos) {
        return new VendaResponse(v.getId(), v.getCaixa().getId(), v.getStatus(),
                v.getItens().stream().map(Item::de).toList(),
                v.getPagamentos().stream().map(PagamentoResponse::de).toList(),
                v.getItens().size(), v.getTotal(), v.getValorPago(), v.getRestante(), v.getTroco(),
                v.getDocumentoConsumidor(), v.getDataAbertura(), v.getDataFinalizacao(), v.getDataCancelamento(),
                v.getMotivoCancelamento(), nota, avisos);
    }
}
