package br.com.balcao.pdv.cliente;

import br.com.balcao.pdv.usuario.Contexto;
import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Requer;
import br.com.balcao.pdv.venda.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;
    private final Contexto contexto;

    public record ClienteResponse(Long id, String nome, String documento, String telefone, String email,
                                  BigDecimal limiteCredito, BigDecimal saldoDevedor, BigDecimal creditoDisponivel,
                                  String observacao, boolean ativo, OffsetDateTime criadoEm) {
        public static ClienteResponse de(Cliente c) {
            return new ClienteResponse(c.getId(), c.getNome(), c.getDocumento(), c.getTelefone(), c.getEmail(),
                    c.getLimiteCredito(), c.getSaldoDevedor(), c.getCreditoDisponivel(), c.getObservacao(),
                    c.isAtivo(), c.getCriadoEm());
        }
    }

    public record LancamentoResponse(Long id, LancamentoCliente.Tipo tipo, BigDecimal valor, String forma,
                                     Long vendaId, String observacao, BigDecimal saldoApos, OffsetDateTime dataHora) {
        static LancamentoResponse de(LancamentoCliente l) {
            return new LancamentoResponse(l.getId(), l.getTipo(), l.getValor(), l.getForma(), l.getVendaId(),
                    l.getObservacao(), l.getSaldoApos(), l.getDataHora());
        }
    }

    public record RecebimentoRequest(@NotNull BigDecimal valor, @NotNull FormaPagamento forma,
                                     @Size(max = 255) String observacao) {
    }

    @GetMapping
    public Page<ClienteResponse> pesquisar(@RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "false") boolean todos,
                                           @RequestParam(defaultValue = "false") boolean devedores,
                                           @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC)
                                           Pageable pageable) {
        return service.pesquisar(q, todos, devedores, pageable).map(ClienteResponse::de);
    }

    @GetMapping("/a-receber")
    public Map<String, BigDecimal> aReceber() {
        return Map.of("total", service.totalAReceber());
    }

    @GetMapping("/{id}")
    public ClienteResponse porId(@PathVariable Long id) {
        return ClienteResponse.de(service.buscar(id));
    }

    /** Cadastro rápido pelo caixa é liberado; limite de crédito acima de zero exige gerente. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse criar(@RequestBody ClienteService.ClienteRequest req) {
        if (req.limiteCredito() != null && req.limiteCredito().signum() > 0) {
            contexto.exigirGerente("Dar limite de fiado");
        }
        return ClienteResponse.de(service.salvar(null, req));
    }

    @PutMapping("/{id}")
    public ClienteResponse atualizar(@PathVariable Long id, @RequestBody ClienteService.ClienteRequest req) {
        Cliente atual = service.buscar(id);
        if (req.limiteCredito() != null && req.limiteCredito().compareTo(atual.getLimiteCredito()) != 0) {
            contexto.exigirGerente("Alterar limite de fiado");
        }
        return ClienteResponse.de(service.salvar(id, req));
    }

    @GetMapping("/{id}/extrato")
    public Page<LancamentoResponse> extrato(@PathVariable Long id, @PageableDefault(size = 50) Pageable pageable) {
        return service.extrato(id, pageable).map(LancamentoResponse::de);
    }

    @PostMapping("/{id}/recebimentos")
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponse receber(@PathVariable Long id, @Valid @RequestBody RecebimentoRequest req) {
        return LancamentoResponse.de(service.receber(id, req.valor(), req.forma(), req.observacao(),
                contexto.operadorId()));
    }

    @Requer(Papel.GERENTE)
    @PatchMapping("/{id}/ativo")
    public ClienteResponse ativar(@PathVariable Long id, @RequestParam boolean ativo) {
        Cliente c = service.buscar(id);
        return ClienteResponse.de(service.salvar(id, new ClienteService.ClienteRequest(c.getNome(), c.getDocumento(),
                c.getTelefone(), c.getEmail(), c.getLimiteCredito(), c.getObservacao(), ativo)));
    }
}
