/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.caixa;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface CaixaRepository extends JpaRepository<Caixa, Long> {

    Optional<Caixa> findFirstByStatus(StatusCaixa status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caixa c where c.id = :id")
    Optional<Caixa> travarPorId(@Param("id") Long id);

    @Query("""
            select c from Caixa c
            where c.dataAbertura >= :inicio and c.dataAbertura < :fim
            order by c.dataAbertura desc
            """)
    Page<Caixa> porPeriodo(@Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim,
                           Pageable pageable);
}
