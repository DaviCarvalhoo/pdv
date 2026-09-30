/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.loja;

import br.com.balcao.pdv.estoque.PoliticaSaldoInsuficiente;
import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Requer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/loja")
@RequiredArgsConstructor
public class LojaController {

    private final LojaService service;

    /** Só o necessário para a tela de login (sem autenticação). */
    public record LojaPublica(String nomeFantasia, String slogan, String logo, String corDestaque) {
    }

    public record LojaResponse(String nomeFantasia, String slogan, String logo, String corDestaque,
                               String mensagemCupom, PoliticaSaldoInsuficiente politicaEstoque,
                               BigDecimal limiteGaveta, BigDecimal descontoMaxOperador, String balancaPrefixo,
                               Integer balancaDigitosCodigo, TipoValorBalanca balancaTipoValor, String chavePix,
                               String pixRecebedor, String pixCidade, boolean pixConfigurado,
                               BigDecimal aliquotaTributos, Integer bloqueioInatividadeMin) {
        static LojaResponse de(Loja l) {
            return new LojaResponse(l.getNomeFantasia(), l.getSlogan(), l.getLogo(), l.getCorDestaque(),
                    l.getMensagemCupom(), l.getPoliticaEstoque(), l.getLimiteGaveta(), l.getDescontoMaxOperador(),
                    l.getBalancaPrefixo(), l.getBalancaDigitosCodigo(), l.getBalancaTipoValor(), l.getChavePix(),
                    l.getPixRecebedor(), l.getPixCidade(), l.getChavePix() != null, l.getAliquotaTributos(),
                    l.getBloqueioInatividadeMin());
        }
    }

    @GetMapping("/publica")
    public LojaPublica publica() {
        Loja l = service.obter();
        return new LojaPublica(l.getNomeFantasia(), l.getSlogan(), l.getLogo(), l.getCorDestaque());
    }

    @GetMapping
    public LojaResponse obter() {
        return LojaResponse.de(service.obter());
    }

    @PutMapping
    @Requer(Papel.ADMIN)
    public LojaResponse atualizar(@RequestBody LojaService.LojaRequest req) {
        return LojaResponse.de(service.atualizar(req));
    }
}
