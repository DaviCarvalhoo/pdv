package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.caixa.Caixa;
import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.estoque.EstoqueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Troca e devolução parcial de vendas finalizadas, e o ciclo de vida do vale-troca.
 * A devolução não mexe na NFC-e original (a nota de devolução fiscal fica com o contador).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrocaService {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem 0/O e 1/I
    private final SecureRandom aleatorio = new SecureRandom();

    private final VendaRepository vendaRepository;
    private final ValeTrocaRepository valeRepository;
    private final DevolucaoRepository devolucaoRepository;
    private final EstoqueService estoqueService;
    private final CaixaService caixaService;
    private final Clock relogio;

    public record ItemDevolvido(Long itemId, BigDecimal quantidade) {
    }

    public record Resultado(Long devolucaoId, Long vendaId, BigDecimal valor, Devolucao.Destino destino,
                            String codigoVale, OffsetDateTime dataHora) {
        static Resultado de(Devolucao d) {
            return new Resultado(d.getId(), d.getVendaId(), d.getValor(), d.getDestino(),
                    d.getValeTroca() != null ? d.getValeTroca().getCodigo() : null, d.getDataHora());
        }
    }

    @Transactional
    public Resultado devolver(Long vendaId, List<ItemDevolvido> itens, Devolucao.Destino destino, String motivo,
                              Long operadorId) {
        Venda venda = vendaRepository.findById(vendaId).orElseThrow(() -> new NaoEncontradoException("Venda", vendaId));
        if (venda.getStatus() != StatusVenda.FINALIZADA) {
            throw new RegraNegocioException("VENDA_NAO_FINALIZADA", "Só vendas finalizadas aceitam troca ou devolução.");
        }
        if (itens == null || itens.stream().noneMatch(i -> i.quantidade() != null && i.quantidade().signum() > 0)) {
            throw new RegraNegocioException("SEM_ITENS", "Escolha ao menos um item para devolver.");
        }
        if (destino == null) {
            throw new RegraNegocioException("DESTINO_OBRIGATORIO", "Escolha entre vale-troca e dinheiro.");
        }

        // Valor líquido de cada item (com o desconto da venda rateado), proporcional à quantidade devolvida.
        List<BigDecimal> descontos = Rateio.descontos(venda);
        BigDecimal valor = BigDecimal.ZERO;
        for (ItemDevolvido d : itens) {
            if (d.quantidade() == null || d.quantidade().signum() <= 0) {
                continue;
            }
            int indice = indiceDoItem(venda, d.itemId());
            ItemVenda item = venda.getItens().get(indice);
            if (d.quantidade().compareTo(item.getQuantidadeDevolvivel()) > 0) {
                throw new RegraNegocioException("QUANTIDADE_DEVOLVIDA_INVALIDA",
                        "De " + item.getDescricao() + " só podem voltar " + item.getQuantidadeDevolvivel()
                                .stripTrailingZeros().toPlainString() + ".");
            }
            BigDecimal liquido = item.getSubtotal().subtract(descontos.get(indice));
            valor = valor.add(liquido.multiply(d.quantidade()).divide(item.getQuantidade(), 2, RoundingMode.HALF_EVEN));
            item.registrarDevolucao(Dinheiro.quantidade(d.quantidade()));
            estoqueService.devolucao(item.getProduto().getId(), d.quantidade(), vendaId);
        }
        valor = Dinheiro.valor(valor);
        if (valor.signum() <= 0) {
            throw new RegraNegocioException("VALOR_INVALIDO", "O valor da devolução ficou zerado.");
        }

        ValeTroca vale = null;
        if (destino == Devolucao.Destino.DINHEIRO) {
            Caixa caixa = caixaService.travar(caixaService.exigirAberto().getId());
            caixaService.registrarDevolucao(caixa, valor, vendaId, operadorId);
        } else {
            vale = valeRepository.save(new ValeTroca(novoCodigo(), valor, vendaId,
                    venda.getCliente() != null ? venda.getCliente().getId() : null, operadorId, agora()));
        }
        Devolucao d = devolucaoRepository.save(new Devolucao(vendaId, valor, destino, vale,
                StringUtils.hasText(motivo) ? motivo.trim() : null, operadorId, agora()));
        log.info("Devolução de {} na venda #{} ({})", valor, vendaId, destino);
        return Resultado.de(d);
    }

    @Transactional(readOnly = true)
    public List<Resultado> devolucoes(Long vendaId) {
        return devolucaoRepository.findByVendaIdOrderByDataHora(vendaId).stream().map(Resultado::de).toList();
    }

    @Transactional(readOnly = true)
    public ValeTroca vale(String codigo) {
        return valeRepository.findByCodigo(normalizar(codigo))
                .orElseThrow(() -> new NaoEncontradoException("Vale", codigo));
    }

    /** Confere se o vale cobre o valor pedido nesta venda (somando o que a venda já usou dele). */
    @Transactional(readOnly = true)
    public void validarUso(Venda venda, String codigo, BigDecimal valor) {
        ValeTroca vale = vale(codigo);
        BigDecimal jaUsado = venda.getPagamentos().stream()
                .filter(p -> p.getForma() == FormaPagamento.VALE_TROCA && normalizar(p.getIdentificadorTransacao()).equals(vale.getCodigo()))
                .map(Pagamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disponivel = vale.getSaldo().subtract(jaUsado);
        if (valor.compareTo(disponivel) > 0) {
            throw new RegraNegocioException("VALE_SEM_SALDO",
                    "O vale-troca " + vale.getCodigo() + " tem " + disponivel.max(BigDecimal.ZERO) + " de saldo.",
                    Map.of("saldo", disponivel.max(BigDecimal.ZERO)));
        }
    }

    /** Na finalização: debita os vales usados como pagamento. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void consumirVales(Venda venda) {
        for (Pagamento p : venda.getPagamentos()) {
            if (p.getForma() == FormaPagamento.VALE_TROCA) {
                travar(p.getIdentificadorTransacao()).debitar(p.getValor());
            }
        }
    }

    /** No estorno: devolve o saldo aos vales usados. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void devolverVales(Venda venda) {
        for (Pagamento p : venda.getPagamentos()) {
            if (p.getForma() == FormaPagamento.VALE_TROCA) {
                travar(p.getIdentificadorTransacao()).creditar(p.getValor());
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean temDevolucao(Long vendaId) {
        return devolucaoRepository.existsByVendaId(vendaId);
    }

    static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase().replace(" ", "");
    }

    private ValeTroca travar(String codigo) {
        return valeRepository.travarPorCodigo(normalizar(codigo))
                .orElseThrow(() -> new NaoEncontradoException("Vale", codigo));
    }

    private static int indiceDoItem(Venda venda, Long itemId) {
        for (int i = 0; i < venda.getItens().size(); i++) {
            if (Objects.equals(venda.getItens().get(i).getId(), itemId)) {
                return i;
            }
        }
        throw new NaoEncontradoException("Item", itemId);
    }

    private String novoCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("VT");
            for (int i = 0; i < 6; i++) {
                sb.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
            }
            codigo = sb.toString();
        } while (valeRepository.existsByCodigo(codigo));
        return codigo;
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(relogio);
    }
}
