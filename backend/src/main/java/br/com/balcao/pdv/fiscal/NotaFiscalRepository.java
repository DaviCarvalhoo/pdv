package br.com.balcao.pdv.fiscal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {

    Optional<NotaFiscal> findFirstByVendaIdOrderByIdDesc(Long vendaId);

    boolean existsByVendaIdAndStatus(Long vendaId, StatusNota status);

    @Query("""
            select n from NotaFiscal n
            where n.dataEmissao >= :inicio and n.dataEmissao < :fim
              and (:status is null or n.status = :status)
            order by n.dataEmissao desc, n.id desc
            """)
    Page<NotaFiscal> pesquisar(@Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim,
                               @Param("status") StatusNota status, Pageable pageable);
}
