package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.cliente.Cliente;
import br.com.balcao.pdv.fiscal.NotaFiscalResumo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record VendaResponse(
        Long id, Long caixaId, StatusVenda status,
        List<Item> itens, List<PagamentoResponse> pagamentos,
        int quantidadeItens, BigDecimal subtotal, BigDecimal desconto, BigDecimal descontoPercentual,
        BigDecimal total, BigDecimal valorPago, BigDecimal restante, BigDecimal troco, BigDecimal tributosAprox,
        String documentoConsumidor, ClienteVenda cliente, String operadorNome,
        boolean emEspera, String identificacao,
        OffsetDateTime dataAbertura, OffsetDateTime dataFinalizacao, OffsetDateTime dataCancelamento,
        String motivoCancelamento,
        NotaFiscalResumo notaFiscal,
        List<String> avisos) {

    public record Item(Long id, Long produtoId, String codigo, String descricao, String unidade,
                       BigDecimal precoUnitario, BigDecimal precoNormal, boolean promocional,
                       BigDecimal quantidade, BigDecimal subtotal, BigDecimal estoqueDisponivel) {
        static Item de(ItemVenda i) {
            var p = i.getProduto();
            String codigo = p.getGtin() != null ? p.getGtin() : p.getCodigoInterno();
            return new Item(i.getId(), p.getId(), codigo, i.getDescricao(), p.getUnidade(), i.getPrecoUnitario(),
                    p.getPreco(), i.isPromocional(), i.getQuantidade(), i.getSubtotal(), p.getEstoqueAtual());
        }
    }

    public record PagamentoResponse(Long id, FormaPagamento forma, BigDecimal valor, String identificadorTransacao,
                                    OffsetDateTime dataHora) {
        static PagamentoResponse de(Pagamento p) {
            return new PagamentoResponse(p.getId(), p.getForma(), p.getValor(), p.getIdentificadorTransacao(),
                    p.getDataHora());
        }
    }

    public record ClienteVenda(Long id, String nome, String documento, BigDecimal saldoDevedor,
                               BigDecimal creditoDisponivel) {
        static ClienteVenda de(Cliente c) {
            return c == null ? null : new ClienteVenda(c.getId(), c.getNome(), c.getDocumento(), c.getSaldoDevedor(),
                    c.getCreditoDisponivel());
        }
    }

    public static VendaResponse de(Venda v, NotaFiscalResumo nota, List<String> avisos) {
        return new VendaResponse(v.getId(), v.getCaixa().getId(), v.getStatus(),
                v.getItens().stream().map(Item::de).toList(),
                v.getPagamentos().stream().map(PagamentoResponse::de).toList(),
                v.getItens().size(), v.getSubtotal(), v.getDesconto(), v.getDescontoPercentual(),
                v.getTotal(), v.getValorPago(), v.getRestante(), v.getTroco(), v.getTributosAprox(),
                v.getDocumentoConsumidor(), ClienteVenda.de(v.getCliente()),
                v.getOperador() != null ? v.getOperador().getNome() : null,
                v.isEmEspera(), v.getIdentificacao(),
                v.getDataAbertura(), v.getDataFinalizacao(), v.getDataCancelamento(),
                v.getMotivoCancelamento(), nota, avisos);
    }

    /** Mesma resposta com avisos extras (ex.: resultado da NFC-e). */
    public VendaResponse comAvisos(List<String> extras) {
        List<String> todos = new java.util.ArrayList<>(extras);
        todos.addAll(avisos);
        return new VendaResponse(id, caixaId, status, itens, pagamentos, quantidadeItens, subtotal, desconto,
                descontoPercentual, total, valorPago, restante, troco, tributosAprox, documentoConsumidor, cliente,
                operadorNome, emEspera, identificacao, dataAbertura, dataFinalizacao, dataCancelamento,
                motivoCancelamento, notaFiscal, todos);
    }
}
