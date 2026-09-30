package br.com.balcao.pdv.fiscal;

import br.com.balcao.pdv.usuario.Contexto;
import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Requer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/fiscal")
@RequiredArgsConstructor
public class FiscalController {

    private final ConfiguracaoFiscalService configuracaoService;
    private final NfceService nfceService;
    private final Contexto contexto;

    public record CancelamentoRequest(@NotBlank(message = "Informe a justificativa") String justificativa) {
    }

    @GetMapping("/configuracao")
    public ConfiguracaoFiscalDto.Response configuracao() {
        ConfiguracaoFiscal c = configuracaoService.obter();
        return ConfiguracaoFiscalDto.Response.de(c, configuracaoService.pendencias(c));
    }

    @Requer(Papel.ADMIN)
    @PutMapping("/configuracao")
    public ConfiguracaoFiscalDto.Response atualizar(@Valid @RequestBody ConfiguracaoFiscalDto.Request req) {
        ConfiguracaoFiscal c = configuracaoService.atualizar(req);
        return ConfiguracaoFiscalDto.Response.de(c, configuracaoService.pendencias(c));
    }

    @PostMapping("/vendas/{vendaId}/nfce")
    public NotaFiscalResumo emitir(@PathVariable Long vendaId) {
        return nfceService.emitir(vendaId);
    }

    @GetMapping("/notas")
    public Page<NotaFiscalResumo> notas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusNota status,
            @PageableDefault(size = 20) Pageable pageable) {
        ZoneId zona = ZoneId.systemDefault();
        LocalDate de = inicio != null ? inicio : LocalDate.now().minusDays(30);
        LocalDate ate = fim != null ? fim : LocalDate.now();
        return nfceService.pesquisar(de.atStartOfDay(zona).toOffsetDateTime(),
                ate.plusDays(1).atStartOfDay(zona).toOffsetDateTime(), status, pageable);
    }

    @GetMapping("/notas/{id}")
    public NotaFiscalResumo nota(@PathVariable Long id) {
        return nfceService.resumo(id);
    }

    @GetMapping("/notas/{id}/danfe")
    public DanfeNfce danfe(@PathVariable Long id) {
        return nfceService.danfe(id);
    }

    @GetMapping(value = "/notas/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> xml(@PathVariable Long id) {
        NotaFiscal nota = nfceService.comXml(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"NFCe" + nota.getChaveAcesso() + ".xml\"")
                .body(nota.getXml());
    }

    @PostMapping("/notas/{id}/cancelar")
    public NotaFiscalResumo cancelar(@PathVariable Long id, @Valid @RequestBody CancelamentoRequest req) {
        contexto.exigirGerente("Cancelar NFC-e");
        return nfceService.cancelar(id, req.justificativa());
    }
}
