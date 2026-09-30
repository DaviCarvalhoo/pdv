package br.com.balcao.pdv.venda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VendaRepository extends JpaRepository<Venda, Long>, JpaSpecificationExecutor<Venda> {

    /** A venda que está na tela (aberta e não estacionada). */
    Optional<Venda> findFirstByCaixaIdAndStatusAndEmEsperaFalseOrderByIdDesc(Long caixaId, StatusVenda status);

    List<Venda> findByCaixaIdAndStatusAndEmEsperaTrueOrderByIdAsc(Long caixaId, StatusVenda status);

    long countByCaixaIdAndStatus(Long caixaId, StatusVenda status);

    @Query("select v.id from Venda v where v.caixa.id = :caixaId and v.status = :status order by v.id")
    List<Long> idsPorCaixaEStatus(@Param("caixaId") Long caixaId, @Param("status") StatusVenda status);

    @Query("""
            select p.forma, sum(p.valor) from Pagamento p
            where p.venda.caixa.id = :caixaId and p.venda.status = :status
            group by p.forma
            """)
    List<Object[]> totaisPorForma(@Param("caixaId") Long caixaId, @Param("status") StatusVenda status);

    @Query("select coalesce(sum(v.troco), 0) from Venda v where v.caixa.id = :caixaId and v.status = :status")
    BigDecimal somaTroco(@Param("caixaId") Long caixaId, @Param("status") StatusVenda status);

    @Query("select coalesce(sum(v.total), 0) from Venda v where v.caixa.id = :caixaId and v.status = :status")
    BigDecimal somaTotal(@Param("caixaId") Long caixaId, @Param("status") StatusVenda status);
}
