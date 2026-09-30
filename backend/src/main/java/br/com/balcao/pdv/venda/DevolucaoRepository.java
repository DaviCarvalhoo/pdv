package br.com.balcao.pdv.venda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevolucaoRepository extends JpaRepository<Devolucao, Long> {

    List<Devolucao> findByVendaIdOrderByDataHora(Long vendaId);

    boolean existsByVendaId(Long vendaId);
}
