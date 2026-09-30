package br.com.balcao.pdv.estoque;

import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.produto.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Único ponto que altera {@code Produto.estoqueAtual}. Toda alteração grava uma movimentação,
 * de modo que o saldo sempre é igual à soma do histórico (RN-EST-01 e RN-EST-02).
 */
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository repository;

    @Value("${balcao.estoque.politica-saldo-insuficiente}")
    private PoliticaSaldoInsuficiente politica;

    @Transactional
    public MovimentacaoEstoque entrada(Long produtoId, BigDecimal quantidade, String observacao) {
        if (!Dinheiro.positivo(quantidade)) {
            throw new RegraNegocioException("QUANTIDADE_INVALIDA", "A quantidade de entrada deve ser maior que zero.");
        }
        return movimentar(produtoId, TipoMovimentacaoEstoque.ENTRADA, quantidade, null, observacao);
    }

    @Transactional
    public MovimentacaoEstoque ajuste(Long produtoId, BigDecimal quantidade, String observacao) {
        if (quantidade == null || quantidade.signum() == 0) {
            throw new RegraNegocioException("QUANTIDADE_INVALIDA", "O ajuste não pode ser zero.");
        }
        if (!StringUtils.hasText(observacao)) {
            throw new RegraNegocioException("OBSERVACAO_OBRIGATORIA", "Informe o motivo do ajuste.");
        }
        return movimentar(produtoId, TipoMovimentacaoEstoque.AJUSTE, quantidade, null, observacao);
    }

    /** Baixa de estoque na finalização da venda. Roda na transação da venda. */
    @Transactional(propagation = Propagation.MANDATORY)
    public MovimentacaoEstoque saidaVenda(Long produtoId, BigDecimal quantidade, Long vendaId) {
        Produto produto = travar(produtoId);
        if (politica == PoliticaSaldoInsuficiente.BLOQUEAR && produto.getEstoqueAtual().compareTo(quantidade) < 0) {
            throw new RegraNegocioException("ESTOQUE_INSUFICIENTE",
                    "Estoque insuficiente para " + produto.getNome() + ".",
                    Map.of("produtoId", produtoId, "disponivel", produto.getEstoqueAtual()));
        }
        return registrar(produto, TipoMovimentacaoEstoque.SAIDA_VENDA, quantidade.negate(), vendaId,
                "Venda #" + vendaId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public MovimentacaoEstoque estornoVenda(Long produtoId, BigDecimal quantidade, Long vendaId) {
        return movimentar(produtoId, TipoMovimentacaoEstoque.ESTORNO_VENDA, quantidade, vendaId,
                "Estorno da venda #" + vendaId);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> historico(Long produtoId, Pageable pageable) {
        if (!produtoRepository.existsById(produtoId)) {
            throw new NaoEncontradoException("Produto", produtoId);
        }
        return repository.findByProdutoIdOrderByDataHoraDescIdDesc(produtoId, pageable);
    }

    private MovimentacaoEstoque movimentar(Long produtoId, TipoMovimentacaoEstoque tipo, BigDecimal quantidade,
                                           Long vendaId, String observacao) {
        return registrar(travar(produtoId), tipo, quantidade, vendaId, observacao);
    }

    private MovimentacaoEstoque registrar(Produto produto, TipoMovimentacaoEstoque tipo, BigDecimal quantidade,
                                          Long vendaId, String observacao) {
        BigDecimal qtd = Dinheiro.quantidade(quantidade);
        MovimentacaoEstoque mov = new MovimentacaoEstoque(produto, tipo, qtd, produto.getEstoqueAtual(),
                vendaId, StringUtils.hasText(observacao) ? observacao.trim() : null);
        produto.aplicarSaldoEstoque(mov.getSaldoPosterior());
        return repository.save(mov);
    }

    private Produto travar(Long produtoId) {
        return produtoRepository.travarPorId(produtoId)
                .orElseThrow(() -> new NaoEncontradoException("Produto", produtoId));
    }
}
