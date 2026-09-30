package br.com.balcao.pdv.venda;

import br.com.balcao.pdv.fiscal.NfceService;
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

@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
public class VendaController {

    private final VendaService service;
    private final NfceService nfceService;

    public record ItemRequest(Long produtoId, @Size(max = 30) String codigo, BigDecimal quantidade) {
    }

    public record QuantidadeRequest(@NotNull(message = "Informe a quantidade") BigDecimal quantidade) {
    }

    public record PagamentoRequest(@NotNull(message = "Informe a forma de pagamento") FormaPagamento forma,
                                   @NotNull(message = "Informe o valor") BigDecimal valor,
                                   @Size(max = 60) String identificadorTransacao) {
    }

    public record ConsumidorRequest(@Size(max = 18) String documento) {
    }

    public record MotivoRequest(@Size(max = 255) String motivo) {
    }

    public record HistoricoResponse(Page<VendaResumo> vendas, VendaService.Totalizadores totalizadores) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResponse iniciar() {
        return service.iniciar();
    }

    /** 200 com a venda em andamento no caixa atual ou 204 se não houver. */
    @GetMapping("/aberta")
    public ResponseEntity<VendaResponse> emAndamento() {
        return service.emAndamento().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public HistoricoResponse historico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusVenda status,
            @RequestParam(required = false) Long caixaId,
            @RequestParam(required = false) FormaPagamento forma,
            @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        FiltroVendas filtro = new FiltroVendas(inicioDoDia(inicio), fim != null ? inicioDoDia(fim.plusDays(1)) : null,
                status, caixaId, forma);
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

    @PutMapping("/{id}/consumidor")
    public VendaResponse informarConsumidor(@PathVariable Long id, @Valid @RequestBody ConsumidorRequest req) {
        return service.informarConsumidor(id, req.documento());
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
        VendaResponse venda = service.detalhar(id);
        if (avisos.isEmpty()) {
            return venda;
        }
        avisos.addAll(venda.avisos());
        return new VendaResponse(venda.id(), venda.caixaId(), venda.status(), venda.itens(), venda.pagamentos(),
                venda.quantidadeItens(), venda.total(), venda.valorPago(), venda.restante(), venda.troco(),
                venda.documentoConsumidor(), venda.dataAbertura(), venda.dataFinalizacao(), venda.dataCancelamento(),
                venda.motivoCancelamento(), venda.notaFiscal(), avisos);
    }

    @PostMapping("/{id}/cancelar")
    public VendaResponse cancelar(@PathVariable Long id, @Valid @RequestBody(required = false) MotivoRequest req) {
        return service.cancelar(id, req != null ? req.motivo() : null);
    }

    @PostMapping("/{id}/estornar")
    public VendaResponse estornar(@PathVariable Long id, @Valid @RequestBody(required = false) MotivoRequest req) {
        return service.estornar(id, req != null ? req.motivo() : null);
    }

    private static OffsetDateTime inicioDoDia(LocalDate data) {
        return data == null ? null : data.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
