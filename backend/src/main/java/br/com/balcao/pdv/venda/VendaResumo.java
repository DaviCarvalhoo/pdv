package br.com.balcao.pdv.venda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Linha do histórico de vendas (sem itens). */
public record VendaResumo(Long id, Long caixaId, StatusVenda status, int quantidadeItens, BigDecimal total,
                          BigDecimal desconto, BigDecimal troco, List<FormaPagamento> formas, String operadorNome,
                          String clienteNome, OffsetDateTime dataAbertura, OffsetDateTime dataFinalizacao) {

    public static VendaResumo de(Venda v) {
        return new VendaResumo(v.getId(), v.getCaixa().getId(), v.getStatus(), v.getItens().size(), v.getTotal(),
                v.getDesconto(), v.getTroco(), v.getPagamentos().stream().map(Pagamento::getForma).distinct().toList(),
                v.getOperador() != null ? v.getOperador().getNome() : null,
                v.getCliente() != null ? v.getCliente().getNome() : null,
                v.getDataAbertura(), v.getDataFinalizacao());
    }
}
