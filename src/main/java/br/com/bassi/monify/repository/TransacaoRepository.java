package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID>, JpaSpecificationExecutor<Transacao> {

    @EntityGraph(attributePaths = "categoria")
    Optional<Transacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Transacao> findAll(Specification<Transacao> specification, Pageable pageable);

    @Query("""
            select t.categoria.id as categoriaId, sum(t.valor) as total
              from Transacao t
             where t.usuarioId = :usuarioId
               and t.tipo = br.com.bassi.monify.domain.enums.TipoTransacao.DESPESA
               and t.dataOcorrencia between :inicio and :fim
             group by t.categoria.id
            """)
    List<GastoPorCategoria> somarDespesasPorCategoria(UUID usuarioId, LocalDate inicio, LocalDate fim);

    @Query("""
            select coalesce(sum(t.valor), 0)
              from Transacao t
             where t.usuarioId = :usuarioId
               and t.categoria.id = :categoriaId
               and t.dataOcorrencia between :inicio and :fim
            """)
    BigDecimal somarDaCategoria(UUID usuarioId, UUID categoriaId, LocalDate inicio, LocalDate fim);
}
