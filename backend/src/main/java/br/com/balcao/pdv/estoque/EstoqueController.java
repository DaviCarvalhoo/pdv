package br.com.balcao.pdv.estoque;

import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Requer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/produtos/{produtoId}/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService service;

    public record MovimentoRequest(@NotNull(message = "Informe a quantidade") BigDecimal quantidade,
                                   @Size(max = 255) String observacao) {
    }

    public record MovimentacaoResponse(Long id, Long produtoId, String produtoNome, TipoMovimentacaoEstoque tipo,
                                       BigDecimal quantidade, BigDecimal saldoAnterior, BigDecimal saldoPosterior,
                                       Long vendaId, String observacao, OffsetDateTime dataHora) {
        static MovimentacaoResponse de(MovimentacaoEstoque m) {
            return new MovimentacaoResponse(m.getId(), m.getProduto().getId(), m.getProduto().getNome(), m.getTipo(),
                    m.getQuantidade(), m.getSaldoAnterior(), m.getSaldoPosterior(), m.getVendaId(),
                    m.getObservacao(), m.getDataHora());
        }
    }

    @Requer(Papel.GERENTE)
    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse entrada(@PathVariable Long produtoId, @Valid @RequestBody MovimentoRequest req) {
        return MovimentacaoResponse.de(service.entrada(produtoId, req.quantidade(), req.observacao()));
    }

    @Requer(Papel.GERENTE)
    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse ajuste(@PathVariable Long produtoId, @Valid @RequestBody MovimentoRequest req) {
        return MovimentacaoResponse.de(service.ajuste(produtoId, req.quantidade(), req.observacao()));
    }

    @GetMapping("/movimentacoes")
    public Page<MovimentacaoResponse> historico(@PathVariable Long produtoId,
                                                @PageableDefault(size = 30) Pageable pageable) {
        return service.historico(produtoId, pageable).map(MovimentacaoResponse::de);
    }
}
