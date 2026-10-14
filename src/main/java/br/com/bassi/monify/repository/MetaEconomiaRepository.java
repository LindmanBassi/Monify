package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.MetaEconomia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaEconomiaRepository extends JpaRepository<MetaEconomia, UUID> {

    @EntityGraph(attributePaths = {"categoria", "orcamento"})
    Optional<MetaEconomia> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    boolean existsByUsuarioIdAndCategoriaIdAndMesReferenciaAndCanceladaEmIsNull(UUID usuarioId, UUID categoriaId,
                                                                               YearMonth mesReferencia);

    @Query("""
            select m from MetaEconomia m
              join fetch m.categoria c
              left join fetch m.orcamento
             where m.usuarioId = :usuarioId
               and m.mesReferencia = :mesReferencia
             order by c.nome
            """)
    List<MetaEconomia> listarDoMes(UUID usuarioId, YearMonth mesReferencia);

    // metas cujo mês já começou e que ainda aguardam o congelamento da referência
    @Query("""
            select m from MetaEconomia m
              join fetch m.categoria
              left join fetch m.orcamento
             where m.usuarioId = :usuarioId
               and m.canceladaEm is null
               and m.congeladaEm is null
               and m.mesReferencia <= :mesAtual
            """)
    List<MetaEconomia> listarParaCongelar(UUID usuarioId, YearMonth mesAtual);

    List<MetaEconomia> findByUsuarioIdAndCategoriaIdAndCanceladaEmIsNull(UUID usuarioId, UUID categoriaId);

    List<MetaEconomia> findByUsuarioIdAndOrcamentoId(UUID usuarioId, UUID orcamentoId);
}
