package br.com.balcao.pdv.fiscal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConfiguracaoFiscalRepository extends JpaRepository<ConfiguracaoFiscal, Long> {

    /** RN-NFC-02: numeração sem buracos — só uma emissão por vez reserva número. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfiguracaoFiscal c where c.id = :id")
    Optional<ConfiguracaoFiscal> travar(@Param("id") Long id);
}
