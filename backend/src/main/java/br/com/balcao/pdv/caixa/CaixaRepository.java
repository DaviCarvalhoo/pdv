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
