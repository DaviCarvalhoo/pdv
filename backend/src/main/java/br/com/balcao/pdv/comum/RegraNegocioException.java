/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.comum;

import lombok.Getter;

import java.util.Map;

/** Violação de regra de negócio (HTTP 422). */
@Getter
public class RegraNegocioException extends RuntimeException {

    private final String codigo;
    private final Map<String, Object> detalhes;

    public RegraNegocioException(String codigo, String mensagem) {
        this(codigo, mensagem, Map.of());
    }

    public RegraNegocioException(String codigo, String mensagem, Map<String, Object> detalhes) {
        super(mensagem);
        this.codigo = codigo;
        this.detalhes = detalhes;
    }
}
