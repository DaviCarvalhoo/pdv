/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sessao")
@Getter
@NoArgsConstructor
public class Sessao {

    @Id
    private String token;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Usuario usuario;

    private OffsetDateTime criadaEm;
    private OffsetDateTime expiraEm;

    public Sessao(String token, Usuario usuario, OffsetDateTime agora, OffsetDateTime expiraEm) {
        this.token = token;
        this.usuario = usuario;
        this.criadaEm = agora;
        this.expiraEm = expiraEm;
    }
}
