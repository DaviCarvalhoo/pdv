package br.com.balcao.pdv.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoClienteRepository extends JpaRepository<LancamentoCliente, Long> {

    Page<LancamentoCliente> findByClienteIdOrderByDataHoraDescIdDesc(Long clienteId, Pageable pageable);
}
