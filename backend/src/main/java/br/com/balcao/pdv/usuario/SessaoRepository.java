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
