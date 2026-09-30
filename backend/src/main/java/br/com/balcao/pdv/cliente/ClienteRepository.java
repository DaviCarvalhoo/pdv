package br.com.balcao.pdv.cliente;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDocumentoAndIdNot(String documento, Long id);

    @Query("""
            select c from Cliente c
            where (:ativo is null or c.ativo = :ativo)
              and (:devedores = false or c.saldoDevedor > 0)
              and (:termo is null
                   or lower(c.nome) like lower(concat('%', cast(:termo as string), '%'))
                   or c.documento = :termo or c.telefone like concat('%', cast(:termo as string), '%'))
            """)
    Page<Cliente> pesquisar(@Param("termo") String termo, @Param("ativo") Boolean ativo,
                            @Param("devedores") boolean devedores, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> travarPorId(@Param("id") Long id);

    @Query("select coalesce(sum(c.saldoDevedor), 0) from Cliente c where c.saldoDevedor > 0")
    BigDecimal totalAReceber();

    List<Cliente> findTop5ByAtivoTrueAndSaldoDevedorGreaterThanOrderBySaldoDevedorDesc(BigDecimal zero);
}
