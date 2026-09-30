/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.produto;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByGtin(String gtin);

    Optional<Produto> findByCodigoInterno(String codigoInterno);

    boolean existsByGtinAndIdNot(String gtin, Long id);

    boolean existsByCodigoInternoAndIdNot(String codigoInterno, Long id);

    long countByAtivoTrue();

    List<Produto> findByAtivoTrueAndAtalhoRapidoTrueOrderByNome();

    @Query(value = """
            select * from produto
            where codigo_interno ~ '^[0-9]+$'
              and coalesce(nullif(ltrim(codigo_interno, '0'), ''), '0') = :codigo
            limit 1
            """, nativeQuery = true)
    Optional<Produto> porCodigoBalanca(@Param("codigo") String codigo);

    @Query("""
            select p from Produto p
            where p.excluido = false
              and (:ativo is null or p.ativo = :ativo)
              and (:termo is null
                   or lower(p.nome) like lower(concat('%', cast(:termo as string), '%'))
                   or p.gtin = :termo or p.codigoInterno = :termo)
            """)
    Page<Produto> pesquisar(@Param("termo") String termo, @Param("ativo") Boolean ativo, Pageable pageable);

    @Query("""
            select p from Produto p
            where p.ativo = true and p.estoqueMinimo is not null and p.estoqueAtual <= p.estoqueMinimo
            order by p.nome
            """)
    List<Produto> comEstoqueBaixo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> travarPorId(@Param("id") Long id);
}
