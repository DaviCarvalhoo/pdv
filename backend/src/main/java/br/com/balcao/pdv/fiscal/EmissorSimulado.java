/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.fiscal;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Autoriza localmente, sem falar com a SEFAZ. Serve para desenvolvimento, demonstração e treinamento.
 * Por segurança, recusa qualquer nota em ambiente de produção.
 */
@Component
public class EmissorSimulado implements EmissorNfce {

    private final Clock relogio;

    public EmissorSimulado(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public TipoEmissor tipo() {
        return TipoEmissor.SIMULADO;
    }

    @Override
    public Retorno autorizar(NotaFiscal nota, String xml, ConfiguracaoFiscal configuracao) {
        if (nota.getAmbiente() == Ambiente.PRODUCAO) {
            return recusaEmProducao();
        }
        return new Retorno(true, "100", "Autorizado o uso da NF-e (simulado)", protocolo(configuracao),
                OffsetDateTime.now(relogio));
    }

    @Override
    public Retorno cancelar(NotaFiscal nota, String justificativa, ConfiguracaoFiscal configuracao) {
        if (nota.getAmbiente() == Ambiente.PRODUCAO) {
            return recusaEmProducao();
        }
        return new Retorno(true, "135", "Evento registrado e vinculado a NF-e (simulado)", protocolo(configuracao),
                OffsetDateTime.now(relogio));
    }

    private Retorno recusaEmProducao() {
        return new Retorno(false, "999",
                "O emissor SIMULADO não transmite para a SEFAZ. Configure um emissor real para produção.",
                null, OffsetDateTime.now(relogio));
    }

    /** Mesmo formato do protocolo real: 15 dígitos, começando pelo tipo e pelo cUF. */
    private String protocolo(ConfiguracaoFiscal configuracao) {
        long sequencial = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
        return "1" + CodigoUf.de(configuracao.getUf()) + String.format("%02d", OffsetDateTime.now(relogio).getYear() % 100)
                + sequencial;
    }
}
