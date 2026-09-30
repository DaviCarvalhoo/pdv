/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SessaoRepository extends JpaRepository<Sessao, String> {

    Optional<Sessao> findByTokenAndExpiraEmAfter(String token, OffsetDateTime agora);

    @Modifying
    @Query("delete from Sessao s where s.usuario.id = :usuarioId")
    void encerrarDoUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("delete from Sessao s where s.expiraEm < :agora")
    int limparExpiradas(@Param("agora") OffsetDateTime agora);
}
