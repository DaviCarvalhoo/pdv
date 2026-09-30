/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.comum.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Distribui o desconto da venda entre os itens (proporcional ao valor; o último item leva a sobra dos
 * centavos, para a soma bater exatamente) e calcula os tributos aproximados de cada item.
 */
public final class Rateio {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private Rateio() {
    }

    public static List<BigDecimal> descontos(Venda v) {
        List<ItemVenda> itens = v.getItens();
        List<BigDecimal> resultado = new ArrayList<>(itens.size());
        BigDecimal restante = v.getDesconto();
        for (int i = 0; i < itens.size(); i++) {
            BigDecimal parte;
            if (i == itens.size() - 1) {
                parte = restante;
            } else if (v.getSubtotal().signum() == 0) {
                parte = Dinheiro.ZERO;
            } else {
                parte = v.getDesconto().multiply(itens.get(i).getSubtotal())
                        .divide(v.getSubtotal(), 2, RoundingMode.HALF_EVEN);
            }
            resultado.add(parte);
            restante = restante.subtract(parte);
        }
        return resultado;
    }

    /** Tributos aproximados por item, sobre o valor líquido (com o desconto rateado). */
    public static List<BigDecimal> tributos(Venda v, BigDecimal aliquotaPadrao) {
        List<BigDecimal> descontos = descontos(v);
        List<BigDecimal> resultado = new ArrayList<>();
        for (int i = 0; i < v.getItens().size(); i++) {
            ItemVenda item = v.getItens().get(i);
            BigDecimal aliquota = item.getProduto().getAliquotaTributos() != null
                    ? item.getProduto().getAliquotaTributos() : aliquotaPadrao;
            if (aliquota == null) {
                resultado.add(Dinheiro.ZERO);
                continue;
            }
            BigDecimal liquido = item.getSubtotal().subtract(descontos.get(i));
            resultado.add(liquido.multiply(aliquota).divide(CEM, 2, RoundingMode.HALF_EVEN));
        }
        return resultado;
    }

    public static BigDecimal soma(List<BigDecimal> valores) {
        return Dinheiro.valor(valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
