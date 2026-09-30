package br.com.balcao.pdv.caixa;

import br.com.balcao.pdv.venda.FormaPagamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Resumo de fechamento (RF-CX-03 e RF-CX-04). */
public record ExtratoCaixa(
        CaixaResponse caixa,
        BigDecimal saldoInicial,
        BigDecimal suprimentos,
        BigDecimal sangrias,
        BigDecimal vendasDinheiro,
        BigDecimal estornos,
        BigDecimal saldoEsperado,
        BigDecimal valorContado,
        BigDecimal diferenca,
        SituacaoConferencia situacaoConferencia,
        Map<FormaPagamento, BigDecimal> totaisPorForma,
        long vendasFinalizadas,
        long vendasCanceladas,
        long vendasEstornadas,
        BigDecimal totalVendido,
        BigDecimal ticketMedio,
        List<MovimentacaoCaixaResponse> movimentacoes) {
}
