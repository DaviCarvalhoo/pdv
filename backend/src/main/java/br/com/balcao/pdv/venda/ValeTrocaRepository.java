package br.com.balcao.pdv.venda;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ValeTrocaRepository extends JpaRepository<ValeTroca, Long> {

    Optional<ValeTroca> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ValeTroca v where v.codigo = :codigo")
    Optional<ValeTroca> travarPorCodigo(@Param("codigo") String codigo);
}
