package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.Aporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AporteRepository extends JpaRepository<Aporte, UUID> {

    Page<Aporte> findByMetaPoupancaId(UUID metaPoupancaId, Pageable pageable);

    @Query("""
            select a.metaPoupanca.id as metaPoupancaId, sum(a.valor) as total
              from Aporte a
             where a.metaPoupanca.usuarioId = :usuarioId
             group by a.metaPoupanca.id
            """)
    List<AcumuladoPorMeta> somarPorMeta(UUID usuarioId);

    @Query("select coalesce(sum(a.valor), 0) from Aporte a where a.metaPoupanca.id = :metaPoupancaId")
    BigDecimal somarDaMeta(UUID metaPoupancaId);
}
