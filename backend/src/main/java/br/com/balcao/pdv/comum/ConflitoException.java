/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.comum;

import java.util.Map;

/** Operação incompatível com o estado atual do recurso (HTTP 409). */
public class ConflitoException extends RegraNegocioException {

    public ConflitoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }

    public ConflitoException(String codigo, String mensagem, Map<String, Object> detalhes) {
        super(codigo, mensagem, detalhes);
    }
}
