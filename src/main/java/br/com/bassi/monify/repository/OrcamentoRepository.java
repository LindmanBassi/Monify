package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.Orcamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID> {

    @EntityGraph(attributePaths = "categoria")
    Optional<Orcamento> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    Optional<Orcamento> findByUsuarioIdAndCategoriaIdAndMesReferencia(UUID usuarioId, UUID categoriaId, YearMonth mesReferencia);

    @Query("""
            select o from Orcamento o
              join fetch o.categoria c
             where o.usuarioId = :usuarioId
               and o.mesReferencia = :mesReferencia
             order by c.nome
            """)
    List<Orcamento> listarDoMes(UUID usuarioId, YearMonth mesReferencia);
}
