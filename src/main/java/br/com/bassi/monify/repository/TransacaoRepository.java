package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID>, JpaSpecificationExecutor<Transacao> {

    @EntityGraph(attributePaths = "categoria")
    Optional<Transacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Transacao> findAll(Specification<Transacao> specification, Pageable pageable);
}
