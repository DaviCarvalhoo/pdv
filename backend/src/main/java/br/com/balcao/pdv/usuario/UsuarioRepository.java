/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

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
