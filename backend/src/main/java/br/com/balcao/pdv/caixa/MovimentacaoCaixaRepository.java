package br.com.balcao.pdv.caixa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MovimentacaoCaixaRepository extends JpaRepository<MovimentacaoCaixa, Long> {

    List<MovimentacaoCaixa> findByCaixaIdOrderByDataHoraAscIdAsc(Long caixaId);

    @Query("select coalesce(sum(m.valor), 0) from MovimentacaoCaixa m where m.caixa.id = :caixaId and m.tipo = :tipo")
    BigDecimal somaPorTipo(@Param("caixaId") Long caixaId, @Param("tipo") TipoMovimentacaoCaixa tipo);
}
