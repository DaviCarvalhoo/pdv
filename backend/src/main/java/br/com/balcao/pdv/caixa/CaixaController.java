package br.com.balcao.pdv.caixa;

import br.com.balcao.pdv.loja.LojaService;
import br.com.balcao.pdv.usuario.Contexto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/caixas")
@RequiredArgsConstructor
public class CaixaController {

    private final CaixaService service;
    private final Contexto contexto;
    private final LojaService lojaService;

    public record AberturaRequest(@NotNull(message = "Informe o saldo inicial") BigDecimal saldoInicial) {
    }

    public record MovimentoRequest(@NotNull(message = "Informe o valor") BigDecimal valor,
                                   @Size(max = 255) String descricao) {
    }

    public record FechamentoRequest(@NotNull(message = "Informe o valor contado") BigDecimal valorContado) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaixaResponse abrir(@Valid @RequestBody AberturaRequest req) {
        Caixa caixa = service.abrir(req.saldoInicial(), contexto.operadorId());
        return resposta(caixa);
    }

    /** 200 com o caixa aberto ou 204 quando não há caixa aberto. */
    @GetMapping("/aberto")
    public ResponseEntity<CaixaResponse> aberto() {
        return service.aberto()
                .map(c -> ResponseEntity.ok(resposta(c)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public Page<CaixaResponse> porPeriodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @PageableDefault(size = 20) Pageable pageable) {
        ZoneId zona = ZoneId.systemDefault();
        LocalDate de = inicio != null ? inicio : LocalDate.now().minusDays(30);
        LocalDate ate = fim != null ? fim : LocalDate.now();
        return service.porPeriodo(de.atStartOfDay(zona).toOffsetDateTime(),
                        ate.plusDays(1).atStartOfDay(zona).toOffsetDateTime(), pageable)
                .map(this::resposta);
    }

    @GetMapping("/{id}")
    public CaixaResponse porId(@PathVariable Long id) {
        return resposta(service.buscar(id));
    }

    @PostMapping("/{id}/suprimentos")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoCaixaResponse suprimento(@PathVariable Long id, @Valid @RequestBody MovimentoRequest req) {
        return MovimentacaoCaixaResponse.de(service.suprimento(id, req.valor(), req.descricao(), contexto.operadorId()));
    }

    @PostMapping("/{id}/sangrias")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoCaixaResponse sangria(@PathVariable Long id, @Valid @RequestBody MovimentoRequest req) {
        contexto.exigirGerente("Sangria");
        return MovimentacaoCaixaResponse.de(service.sangria(id, req.valor(), req.descricao(), contexto.operadorId()));
    }

    @GetMapping("/{id}/movimentacoes")
    public List<MovimentacaoCaixaResponse> movimentacoes(@PathVariable Long id) {
        return service.movimentacoes(id).stream().map(MovimentacaoCaixaResponse::de).toList();
    }

    @PostMapping("/{id}/fechar")
    public ExtratoCaixa fechar(@PathVariable Long id, @Valid @RequestBody FechamentoRequest req) {
        return service.fechar(id, req.valorContado(), contexto.operadorId());
    }

    @GetMapping("/{id}/extrato")
    public ExtratoCaixa extrato(@PathVariable Long id) {
        return service.extrato(id);
    }

    /** Resposta com o saldo esperado e o alerta de sangria (gaveta acima do limite da loja). */
    private CaixaResponse resposta(Caixa c) {
        BigDecimal saldo = c.isAberto() ? service.saldoEsperado(c) : c.getSaldoEsperado();
        BigDecimal limite = lojaService.obter().getLimiteGaveta();
        boolean alerta = c.isAberto() && limite != null && saldo != null && saldo.compareTo(limite) > 0;
        return CaixaResponse.de(c, saldo, alerta ? limite : null);
    }
}
