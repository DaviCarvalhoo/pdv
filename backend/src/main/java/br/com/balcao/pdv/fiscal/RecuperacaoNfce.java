/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.fiscal;

import br.com.balcao.pdv.comum.RegraNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Notas que ficaram para trás. A venda é gravada primeiro e a NFC-e é emitida logo depois, numa transação
 * própria. Se a energia cair (ou a SEFAZ estiver fora) nesse intervalo, a venda fica sem nota ou com a nota
 * PENDENTE. Ao ligar o sistema e a cada 5 minutos, esta rotina emite de novo o que ficou pendente, sem ninguém
 * precisar lembrar. Notas rejeitadas não são reenviadas: precisam de correção no cadastro (tela Fiscal).
 */
@Slf4j
@Component
public class RecuperacaoNfce {

    /** Só olha vendas recentes: nota de venda muito antiga precisa de decisão humana (contingência, prazo). */
    static final int JANELA_DIAS = 3;

    private final JdbcTemplate jdbc;
    private final NfceService nfceService;
    private final ConfiguracaoFiscalService configuracaoService;
    private final Clock relogio;
    private final AtomicBoolean rodando = new AtomicBoolean();

    public RecuperacaoNfce(JdbcTemplate jdbc, NfceService nfceService, ConfiguracaoFiscalService configuracaoService,
                           Clock relogio) {
        this.jdbc = jdbc;
        this.nfceService = nfceService;
        this.configuracaoService = configuracaoService;
        this.relogio = relogio;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoLigar() {
        int emitidas = recuperar();
        if (emitidas > 0) {
            log.warn("Ao ligar: {} NFC-e que tinham ficado pendentes (queda de energia/rede) foram emitidas.", emitidas);
        }
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    public void periodicamente() {
        recuperar();
    }

    /** Vendas finalizadas recentes sem nota, ou com a última nota pendente de transmissão. */
    public List<Long> pendentes() {
        OffsetDateTime desde = OffsetDateTime.now(relogio).minusDays(JANELA_DIAS);
        return jdbc.queryForList("""
                select v.id from venda v
                left join lateral (
                    select n.status from nota_fiscal n where n.venda_id = v.id order by n.id desc limit 1
                ) ultima on true
                where v.status = 'FINALIZADA' and v.data_finalizacao >= ?
                  and (ultima.status is null or ultima.status = 'PENDENTE')
                order by v.id
                """, Long.class, desde);
    }

    /** Quantas vendas esperam nota, só quando a emissão automática está ligada e configurada. */
    public int contarPendentes() {
        return emissaoAtiva() ? pendentes().size() : 0;
    }

    private boolean emissaoAtiva() {
        ConfiguracaoFiscal config = configuracaoService.obter();
        if (!config.isEmissaoAutomatica()) {
            return false;
        }
        try {
            configuracaoService.validarCompleta(config);
            return true;
        } catch (RegraNegocioException e) {
            return false; // fiscal ainda não configurado: nada a recuperar
        }
    }

    /** @return quantas notas saíram autorizadas */
    public int recuperar() {
        if (!rodando.compareAndSet(false, true)) {
            return 0;
        }
        try {
            if (!emissaoAtiva()) {
                return 0;
            }
            int autorizadas = 0;
            for (Long vendaId : pendentes()) {
                try {
                    NotaFiscalResumo nota = nfceService.emitir(vendaId);
                    if (nota.status() == StatusNota.AUTORIZADA) {
                        autorizadas++;
                    } else if (nota.status() == StatusNota.PENDENTE) {
                        break; // SEFAZ/internet fora: tenta tudo de novo na próxima rodada
                    }
                } catch (RegraNegocioException e) {
                    log.warn("NFC-e da venda #{} não pôde ser emitida automaticamente: {}", vendaId, e.getMessage());
                } catch (RuntimeException e) {
                    // Inclui a nota já autorizada por outro caminho (clique em "reemitir" ao mesmo tempo).
                    log.info("NFC-e da venda #{} não recuperada agora: {}", vendaId, e.getMessage());
                }
            }
            return autorizadas;
        } catch (RuntimeException e) {
            log.warn("Recuperação de NFC-e adiada: {}", e.getMessage());
            return 0;
        } finally {
            rodando.set(false);
        }
    }
}
