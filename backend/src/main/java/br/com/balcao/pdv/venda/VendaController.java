package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.fiscal.NfceService;
import br.com.balcao.pdv.usuario.Contexto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
public class VendaController {

    private final VendaService service;
    private final NfceService nfceService;
    private final Contexto contexto;

    public record ItemRequest(Long produtoId, @Size(max = 30) String codigo, BigDecimal quantidade) {
    }

    public record QuantidadeRequest(@NotNull(message = "Informe a quantidade") BigDecimal quantidade) {
    }

    public record PagamentoRequest(@NotNull(message = "Informe a forma de pagamento") FormaPagamento forma,
                                   @NotNull(message = "Informe o valor") BigDecimal valor,
                                   @Size(max = 60) String identificadorTransacao) {
    }

    public record DescontoRequest(BigDecimal valor, BigDecimal percentual) {
    }

    public record ConsumidorRequest(@Size(max = 18) String documento) {
    }

    public record ClienteRequest(Long clienteId) {
    }

    public record EsperaRequest(@Size(max = 40) String identificacao) {
    }

    public record MotivoRequest(@Size(max = 255) String motivo) {
    }

    public record HistoricoResponse(Page<VendaResumo> vendas, VendaService.Totalizadores totalizadores) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResponse iniciar() {
        return service.iniciar(contexto.operadorId());
    }

    /** 200 com a venda em andamento no caixa atual ou 204 se não houver. */
    @GetMapping("/aberta")
    public ResponseEntity<VendaResponse> emAndamento() {
        return service.emAndamento().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/em-espera")
    public List<VendaResumo> emEspera() {
        return service.emEspera();
    }

    @GetMapping
    public HistoricoResponse historico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusVenda status,
            @RequestParam(required = false) Long caixaId,
            @RequestParam(required = false) FormaPagamento forma,
            @RequestParam(required = false) Long operadorId,
            @RequestParam(required = false) Long clienteId,
            @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        FiltroVendas filtro = new FiltroVendas(inicioDoDia(inicio), fim != null ? inicioDoDia(fim.plusDays(1)) : null,
                status, caixaId, forma, operadorId, clienteId);
        return new HistoricoResponse(service.historico(filtro, pageable), service.totalizadores(filtro));
    }

    @GetMapping("/{id}")
    public VendaResponse detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }

    @GetMapping("/{id}/itens")
    public List<VendaResponse.Item> itens(@PathVariable Long id) {
        return service.detalhar(id).itens();
    }

    @PostMapping("/{id}/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResponse adicionarItem(@PathVariable Long id, @Valid @RequestBody ItemRequest req) {
        return service.adicionarItem(id, req.produtoId(), req.codigo(), req.quantidade());
    }

    @PatchMapping("/{id}/itens/{itemId}")
    public VendaResponse alterarQuantidade(@PathVariable Long id, @PathVariable Long itemId,
                                           @Valid @RequestBody QuantidadeRequest req) {
        return service.alterarQuantidade(id, itemId, req.quantidade());
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    public VendaResponse removerItem(@PathVariable Long id, @PathVariable Long itemId) {
        return service.removerItem(id, itemId);
    }

    @PutMapping("/{id}/desconto")
    public VendaResponse desconto(@PathVariable Long id, @RequestBody DescontoRequest req) {
        return service.aplicarDesconto(id, req.valor(), req.percentual(), contexto::temAutorizacaoDeGerente);
    }

    @GetMapping("/{id}/pagamentos")
    public VendaResponse pagamentos(@PathVariable Long id) {
        return service.detalhar(id);
    }

    @PostMapping("/{id}/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResponse adicionarPagamento(@PathVariable Long id, @Valid @RequestBody PagamentoRequest req) {
        return service.adicionarPagamento(id, req.forma(), req.valor(), req.identificadorTransacao());
    }

    @DeleteMapping("/{id}/pagamentos/{pagamentoId}")
    public VendaResponse removerPagamento(@PathVariable Long id, @PathVariable Long pagamentoId) {
        return service.removerPagamento(id, pagamentoId);
    }

    /** BR Code do PIX no valor restante (ou no valor informado). */
    @GetMapping("/{id}/pix")
    public Map<String, Object> pix(@PathVariable Long id, @RequestParam(required = false) BigDecimal valor) {
        return service.pix(id, valor);
    }

    @PutMapping("/{id}/consumidor")
    public VendaResponse informarConsumidor(@PathVariable Long id, @Valid @RequestBody ConsumidorRequest req) {
        return service.informarConsumidor(id, req.documento());
    }

    @PutMapping("/{id}/cliente")
    public VendaResponse vincularCliente(@PathVariable Long id, @RequestBody ClienteRequest req) {
        return service.vincularCliente(id, req.clienteId());
    }

    @PostMapping("/{id}/espera")
    public VendaResponse colocarEmEspera(@PathVariable Long id, @Valid @RequestBody(required = false) EsperaRequest req) {
        return service.colocarEmEspera(id, req != null ? req.identificacao() : null);
    }

    @PostMapping("/{id}/retomar")
    public VendaResponse retomar(@PathVariable Long id) {
        return service.retomar(id);
    }

    /**
     * Finaliza a venda e, se a emissão automática estiver ligada, emite a NFC-e numa transação separada:
     * uma falha na nota não desfaz a venda (RN-NFC-09) e volta como aviso.
     */
    @PostMapping("/{id}/finalizar")
    public VendaResponse finalizar(@PathVariable Long id) {
        service.finalizar(id);
        List<String> avisos = new ArrayList<>();
        nfceService.emitirSeAutomatico(id).ifPresent(avisos::add);
        return service.detalhar(id).comAvisos(avisos);
    }

    /** Cancelar venda com itens exige gerente (ou PIN do gerente). Venda vazia sai sem autorização. */
    @PostMapping("/{id}/cancelar")
    public VendaResponse cancelar(@PathVariable Long id, @Valid @RequestBody(required = false) MotivoRequest req) {
        if (!service.detalhar(id).itens().isEmpty()) {
            contexto.exigirGerente("Cancelar venda");
        }
        return service.cancelar(id, req != null ? req.motivo() : null);
    }

    @PostMapping("/{id}/estornar")
    public VendaResponse estornar(@PathVariable Long id, @Valid @RequestBody(required = false) MotivoRequest req) {
        contexto.exigirGerente("Estornar venda");
        return service.estornar(id, req != null ? req.motivo() : null, contexto.operadorId());
    }

    private static OffsetDateTime inicioDoDia(LocalDate data) {
        return data == null ? null : data.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
