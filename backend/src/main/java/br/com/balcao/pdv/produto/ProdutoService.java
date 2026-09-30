package br.com.balcao.pdv.produto;

import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.estoque.EstoqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository repository;
    private final EstoqueService estoqueService;
    private final CategoriaRepository categoriaRepository;

    @Transactional
    public Produto cadastrar(ProdutoRequest req) {
        Produto produto = new Produto();
        aplicar(produto, req);
        validarUnicidade(produto, 0L);
        repository.save(produto);
        if (Dinheiro.positivo(req.estoqueInicial())) {
            estoqueService.entrada(produto.getId(), req.estoqueInicial(), "Estoque inicial");
        }
        return produto;
    }

    @Transactional
    public Produto atualizar(Long id, ProdutoRequest req) {
        Produto produto = buscar(id);
        aplicar(produto, req);
        validarUnicidade(produto, id);
        return produto;
    }

    @Transactional(readOnly = true)
    public Produto buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NaoEncontradoException("Produto", id));
    }

    @Transactional(readOnly = true)
    public Produto porGtin(String gtin) {
        return repository.findByGtin(gtin)
                .orElseThrow(() -> new NaoEncontradoException("Produto", "GTIN " + gtin));
    }

    @Transactional(readOnly = true)
    public Produto porCodigoInterno(String codigo) {
        return repository.findByCodigoInterno(codigo)
                .orElseThrow(() -> new NaoEncontradoException("Produto", "código interno " + codigo));
    }

    /** Resolve o que veio do leitor ou do teclado: GTIN primeiro, depois código interno. */
    @Transactional(readOnly = true)
    public Produto porCodigo(String codigo) {
        return repository.findByGtin(codigo)
                .or(() -> repository.findByCodigoInterno(codigo))
                .orElseThrow(() -> new NaoEncontradoException("Produto", "código " + codigo));
    }

    @Transactional(readOnly = true)
    public Page<Produto> pesquisar(String termo, Boolean ativo, Pageable pageable) {
        return repository.pesquisar(StringUtils.hasText(termo) ? termo.trim() : null, ativo, pageable);
    }

    @Transactional(readOnly = true)
    public List<Produto> atalhos() {
        return repository.findByAtivoTrueAndAtalhoRapidoTrueOrderByNome();
    }

    /** GTIN ou código interno, sem lançar erro (usado antes de tentar a etiqueta de balança). */
    @Transactional(readOnly = true)
    public Optional<Produto> porCodigoOpcional(String codigo) {
        return repository.findByGtin(codigo).or(() -> repository.findByCodigoInterno(codigo));
    }

    /** Produto pelo código da etiqueta de balança (código interno, ignorando zeros à esquerda). */
    @Transactional(readOnly = true)
    public Optional<Produto> porCodigoBalanca(String codigo) {
        return repository.porCodigoBalanca(semZerosAEsquerda(codigo));
    }

    static String semZerosAEsquerda(String codigo) {
        String s = codigo.replaceFirst("^0+", "");
        return s.isEmpty() ? "0" : s;
    }

    @Transactional(readOnly = true)
    public long contarAtivos() {
        return repository.countByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Produto> comEstoqueBaixo() {
        return repository.comEstoqueBaixo();
    }

    @Transactional
    public Produto desativar(Long id) {
        Produto produto = buscar(id);
        if (!produto.isAtivo()) {
            throw new ConflitoException("PRODUTO_JA_INATIVO", "O produto já está inativo.");
        }
        produto.setAtivo(false);
        return produto;
    }

    @Transactional
    public Produto reativar(Long id) {
        Produto produto = buscar(id);
        if (produto.isAtivo()) {
            throw new ConflitoException("PRODUTO_JA_ATIVO", "O produto já está ativo.");
        }
        produto.setAtivo(true);
        return produto;
    }

    private void aplicar(Produto p, ProdutoRequest req) {
        String gtin = vazioParaNulo(req.gtin());
        if (gtin != null && !Gtin.valido(gtin)) {
            throw new RegraNegocioException("GTIN_INVALIDO",
                    "O dígito verificador do GTIN " + gtin + " não confere.");
        }
        p.setNome(req.nome().trim());
        p.setPreco(Dinheiro.valor(req.preco()));
        p.setCodigoInterno(vazioParaNulo(req.codigoInterno()));
        p.setGtin(gtin);
        p.setUnidade(StringUtils.hasText(req.unidade()) ? req.unidade().trim().toUpperCase() : "UN");
        p.setNcm(vazioParaNulo(req.ncm()));
        p.setCfop(StringUtils.hasText(req.cfop()) ? req.cfop() : "5102");
        p.setOrigem(req.origem() != null ? req.origem() : 0);
        p.setCsosn(StringUtils.hasText(req.csosn()) ? req.csosn() : "102");
        p.setEstoqueMinimo(req.estoqueMinimo() != null ? Dinheiro.quantidade(req.estoqueMinimo()) : null);
        p.setCategoria(req.categoriaId() == null ? null : categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new NaoEncontradoException("Categoria", req.categoriaId())));
        p.setPrecoCusto(req.precoCusto() != null ? Dinheiro.valor(req.precoCusto()) : null);
        if (req.precoPromocional() != null && req.precoPromocional().compareTo(req.preco()) >= 0) {
            throw new RegraNegocioException("PROMOCAO_INVALIDA", "O preço promocional deve ser menor que o preço normal.");
        }
        if (req.promocaoInicio() != null && req.promocaoFim() != null
                && req.promocaoFim().isBefore(req.promocaoInicio())) {
            throw new RegraNegocioException("PROMOCAO_INVALIDA", "O fim da promoção é anterior ao início.");
        }
        boolean temPromocao = req.precoPromocional() != null;
        p.setPrecoPromocional(temPromocao ? Dinheiro.valor(req.precoPromocional()) : null);
        p.setPromocaoInicio(temPromocao ? req.promocaoInicio() : null);
        p.setPromocaoFim(temPromocao ? req.promocaoFim() : null);
        p.setAtalhoRapido(Boolean.TRUE.equals(req.atalhoRapido()));
        p.setAliquotaTributos(req.aliquotaTributos());
    }

    private void validarUnicidade(Produto p, Long id) {
        if (p.getGtin() != null && repository.existsByGtinAndIdNot(p.getGtin(), id)) {
            throw new ConflitoException("GTIN_DUPLICADO", "Já existe um produto com o GTIN " + p.getGtin() + ".");
        }
        if (p.getCodigoInterno() != null && repository.existsByCodigoInternoAndIdNot(p.getCodigoInterno(), id)) {
            throw new ConflitoException("CODIGO_INTERNO_DUPLICADO",
                    "Já existe um produto com o código interno " + p.getCodigoInterno() + ".");
        }
    }

    private static String vazioParaNulo(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
