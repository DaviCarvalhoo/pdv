package br.com.balcao.pdv.estoque;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    Page<MovimentacaoEstoque> findByProdutoIdOrderByDataHoraDescIdDesc(Long produtoId, Pageable pageable);

    @Query("select coalesce(sum(m.quantidade), 0) from MovimentacaoEstoque m where m.produto.id = :produtoId")
    BigDecimal somaPorProduto(@Param("produtoId") Long produtoId);
}
