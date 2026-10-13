package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.TokenRenovacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenRenovacaoRepository extends JpaRepository<TokenRenovacao, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenRenovacao> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update TokenRenovacao t
               set t.revogadoEm = :agora
             where t.usuarioId = :usuarioId
               and t.revogadoEm is null
            """)
    int revogarTodosDoUsuario(UUID usuarioId, Instant agora);
}
