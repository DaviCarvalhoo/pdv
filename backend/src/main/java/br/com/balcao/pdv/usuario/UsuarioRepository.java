package br.com.balcao.pdv.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findByAtivoTrueOrderByNome();

    List<Usuario> findAllByOrderByAtivoDescNome();

    List<Usuario> findByAtivoTrueAndPapelIn(Collection<Papel> papeis);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    long countByAtivoTrueAndPapel(Papel papel);
}
