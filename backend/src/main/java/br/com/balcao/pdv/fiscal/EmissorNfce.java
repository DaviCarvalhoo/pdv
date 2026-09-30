/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

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
