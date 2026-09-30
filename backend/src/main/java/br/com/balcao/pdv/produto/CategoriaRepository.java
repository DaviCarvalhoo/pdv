package br.com.balcao.pdv.produto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNome();

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
