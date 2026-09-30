package br.com.balcao.pdv.relatorio;

import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Requer;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@Requer(Papel.GERENTE)
public class RelatorioController {

    private final RelatorioService service;

    @GetMapping("/painel")
    public RelatorioService.Painel painel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {
        return service.painel(dia != null ? dia : LocalDate.now());
    }

    @GetMapping("/curva-abc")
    public RelatorioService.CurvaAbc curvaAbc(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        LocalDate ate = fim != null ? fim : LocalDate.now();
        return service.curvaAbc(inicio != null ? inicio : ate.minusDays(29), ate);
    }
}
