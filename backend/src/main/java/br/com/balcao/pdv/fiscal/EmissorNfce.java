package br.com.balcao.pdv.fiscal;

import java.time.OffsetDateTime;

/**
 * Porta de saída para a SEFAZ. O restante do sistema não sabe se a nota vai para a SEFAZ direto,
 * para um provedor (API de terceiros) ou para o simulador.
 */
public interface EmissorNfce {

    TipoEmissor tipo();

    /** Transmite a nota. Falhas de comunicação devem sair como exceção; rejeições, como retorno. */
    Retorno autorizar(NotaFiscal nota, String xml, ConfiguracaoFiscal configuracao);

    Retorno cancelar(NotaFiscal nota, String justificativa, ConfiguracaoFiscal configuracao);

    /**
     * @param codigoStatus cStat da SEFAZ (100 = autorizada, 135 = evento registrado)
     */
    record Retorno(boolean sucesso, String codigoStatus, String motivo, String protocolo, OffsetDateTime dataHora) {
    }
}
