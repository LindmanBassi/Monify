package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.MetaPoupanca;
import br.com.bassi.monify.domain.enums.StatusMetaPoupanca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaPoupancaRepository extends JpaRepository<MetaPoupanca, UUID> {

    Optional<MetaPoupanca> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Query("""
            select m from MetaPoupanca m
             where m.usuarioId = :usuarioId
               and (:status is null or m.status = :status)
             order by m.criadoEm desc
            """)
    List<MetaPoupanca> listar(UUID usuarioId, StatusMetaPoupanca status);
}
