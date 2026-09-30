package br.com.balcao.pdv.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Relógio do sistema com deslocamento opcional. Em produção o deslocamento é sempre zero; os dados de
 * demonstração o usam para gerar um histórico passando pelas regras reais (caixa, estoque, fiado).
 */
public class RelogioAjustavel extends Clock {

    private final Clock base;
    private volatile Duration deslocamento = Duration.ZERO;

    public RelogioAjustavel(Clock base) {
        this.base = base;
    }

    public void irPara(Instant momento) {
        this.deslocamento = Duration.between(base.instant(), momento);
    }

    public void voltarAoPresente() {
        this.deslocamento = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return base.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        RelogioAjustavel r = new RelogioAjustavel(base.withZone(zone));
        r.deslocamento = deslocamento;
        return r;
    }

    @Override
    public Instant instant() {
        return base.instant().plus(deslocamento);
    }
}
