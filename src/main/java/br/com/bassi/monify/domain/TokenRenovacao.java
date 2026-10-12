package br.com.bassi.monify.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// Refresh token opaco: no banco fica apenas o hash (SHA-256).
@Entity
@Table(name = "tokens_renovacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenRenovacao extends EntidadeBase {

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public TokenRenovacao(UUID usuarioId, String tokenHash, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
    }

    public boolean isRevogado() {
        return revogadoEm != null;
    }

    public boolean isExpirado(Instant agora) {
        return !agora.isBefore(expiraEm);
    }

    public boolean isValido(Instant agora) {
        return !isRevogado() && !isExpirado(agora);
    }

    public void revogar(Instant agora) {
        if (revogadoEm == null) {
            revogadoEm = agora;
        }
    }
}
