package br.com.balcao.pdv.produto;

import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Requer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService service;

    /** Lista produtos. Por padrão só os ativos; {@code todos=true} inclui os inativos. */
    @GetMapping
    public Page<ProdutoResponse> pesquisar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean todos,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.pesquisar(q, todos ? null : Boolean.TRUE, pageable).map(ProdutoResponse::de);
    }

    @GetMapping("/{id}")
    public ProdutoResponse porId(@PathVariable Long id) {
        return ProdutoResponse.de(service.buscar(id));
    }

    @GetMapping("/gtin/{gtin}")
    public ProdutoResponse porGtin(@PathVariable String gtin) {
        return ProdutoResponse.de(service.porGtin(gtin));
    }

    @GetMapping("/codigo-interno/{codigo}")
    public ProdutoResponse porCodigoInterno(@PathVariable String codigo) {
        return ProdutoResponse.de(service.porCodigoInterno(codigo));
    }

    /** Aceita GTIN ou código interno — usado pelo leitor de código de barras do PDV. */
    @GetMapping("/codigo/{codigo}")
    public ProdutoResponse porCodigo(@PathVariable String codigo) {
        return ProdutoResponse.de(service.porCodigo(codigo));
    }

    @GetMapping("/contagem-ativos")
    public Map<String, Long> contagemAtivos() {
        return Map.of("ativos", service.contarAtivos());
    }

    /** Botões de acesso rápido do PDV. */
    @GetMapping("/atalhos")
    public List<ProdutoResponse> atalhos() {
        return service.atalhos().stream().map(ProdutoResponse::de).toList();
    }

    @GetMapping("/estoque-baixo")
    public List<ProdutoResponse> estoqueBaixo() {
        return service.comEstoqueBaixo().stream().map(ProdutoResponse::de).toList();
    }

    @Requer(Papel.GERENTE)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse cadastrar(@Valid @RequestBody ProdutoRequest req) {
        return ProdutoResponse.de(service.cadastrar(req));
    }

    @Requer(Papel.GERENTE)
    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest req) {
        return ProdutoResponse.de(service.atualizar(id, req));
    }

    @Requer(Papel.GERENTE)
    @PatchMapping("/{id}/desativar")
    public ProdutoResponse desativar(@PathVariable Long id) {
        return ProdutoResponse.de(service.desativar(id));
    }

    @Requer(Papel.GERENTE)
    @PatchMapping("/{id}/reativar")
    public ProdutoResponse reativar(@PathVariable Long id) {
        return ProdutoResponse.de(service.reativar(id));
    }
}
