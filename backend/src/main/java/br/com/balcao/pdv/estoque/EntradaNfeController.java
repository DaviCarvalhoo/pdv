package br.com.balcao.pdv.estoque;

import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Requer;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estoque/nfe")
@RequiredArgsConstructor
@Requer(Papel.GERENTE)
@Validated
public class EntradaNfeController {

    private final EntradaNfeService service;

    public record XmlRequest(String xml) {
    }

    public record ConfirmacaoRequest(@NotEmpty List<EntradaNfeService.ItemEntrada> itens, String referencia) {
    }

    /** Lê o XML e devolve a prévia (nada é gravado). */
    @PostMapping("/ler")
    public EntradaNfeService.Previa ler(@RequestBody XmlRequest req) {
        return service.ler(req.xml());
    }

    @PostMapping("/confirmar")
    public EntradaNfeService.Resultado confirmar(@RequestBody ConfirmacaoRequest req) {
        return service.confirmar(req.itens(), req.referencia());
    }
}
