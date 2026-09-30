package br.com.balcao.pdv.venda;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Filtros do histórico de vendas (RF-HIS-01). */
public record FiltroVendas(OffsetDateTime inicio, OffsetDateTime fim, StatusVenda status, Long caixaId,
                           FormaPagamento forma) {

    public Specification<Venda> especificacao() {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (inicio != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("dataAbertura"), inicio));
            }
            if (fim != null) {
                p.add(cb.lessThan(root.get("dataAbertura"), fim));
            }
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (caixaId != null) {
                p.add(cb.equal(root.join("caixa", JoinType.INNER).get("id"), caixaId));
            }
            if (forma != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                var pag = sub.from(Pagamento.class);
                sub.select(pag.get("id")).where(cb.equal(pag.get("venda"), root), cb.equal(pag.get("forma"), forma));
                p.add(cb.exists(sub));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
