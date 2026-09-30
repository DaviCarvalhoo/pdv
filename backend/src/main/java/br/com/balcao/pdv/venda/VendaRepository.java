/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

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
