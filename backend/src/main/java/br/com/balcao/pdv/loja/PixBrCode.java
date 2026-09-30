/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.loja;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;

/**
 * PIX "copia e cola" estático (BR Code, padrão EMV MPM do Banco Central) com o valor da venda.
 * Qualquer app de banco lê o QR Code; a conferência do recebimento continua manual.
 */
public final class PixBrCode {

    private PixBrCode() {
    }

    public static String gerar(String chave, String recebedor, String cidade, BigDecimal valor, String txid) {
        // Campos do Manual do BR Code: cada um é ID + tamanho (2 dígitos) + valor.
        String conta = campo("00", "br.gov.bcb.pix") + campo("01", chave.trim());
        String adicional = campo("05", txid == null || txid.isBlank() ? "***"
                : limpar(txid, 25).replaceAll("[^A-Za-z0-9]", ""));
        String payload = campo("00", "01")
                + campo("26", conta)
                + campo("52", "0000")
                + campo("53", "986")
                + (valor != null ? campo("54", valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString()) : "")
                + campo("58", "BR")
                + campo("59", limpar(recebedor, 25))
                + campo("60", limpar(cidade, 15))
                + campo("62", adicional)
                + "6304";
        return payload + crc16(payload);
    }

    private static String campo(String id, String valor) {
        return id + String.format("%02d", valor.length()) + valor;
    }

    /** O BR Code aceita só ASCII: remove acentos e corta no tamanho máximo. */
    static String limpar(String texto, int max) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 .\\-@]", "");
        return semAcento.length() > max ? semAcento.substring(0, max) : semAcento;
    }

    /** CRC16-CCITT (polinômio 0x1021, inicial 0xFFFF), exigido no campo 63. */
    static String crc16(String payload) {
        int crc = 0xFFFF;
        for (byte b : payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return String.format("%04X", crc & 0xFFFF);
    }
}
