package br.com.bassi.monify.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenRenovacaoTest {

    private static final Instant EXPIRA_EM = Instant.parse("2026-10-16T12:00:00Z");

    private final TokenRenovacao token = new TokenRenovacao(UUID.randomUUID(), "hash", EXPIRA_EM);

    @Nested
    class isValido {

        @Test
        @DisplayName("Deve ser válido antes de expirar")
        void deveSerValidoAntesDeExpirar() {

            //Act & Assert
            assertTrue(token.isValido(EXPIRA_EM.minusSeconds(1)));
        }

        @Test
        @DisplayName("Deve ser inválido a partir do instante de expiração")
        void deveSerInvalidoAPartirDaExpiracao() {

            //Act & Assert
            assertTrue(token.isExpirado(EXPIRA_EM));
            assertFalse(token.isValido(EXPIRA_EM));
        }

        @Test
        @DisplayName("Deve ser inválido quando revogado")
        void deveSerInvalidoQuandoRevogado() {

            //Arrange
            token.revogar(EXPIRA_EM.minusSeconds(60));

            //Act & Assert
            assertTrue(token.isRevogado());
            assertFalse(token.isValido(EXPIRA_EM.minusSeconds(30)));
        }
    }

    @Nested
    class revogar {

        @Test
        @DisplayName("Deve manter o instante da primeira revogação")
        void deveManterInstanteDaPrimeiraRevogacao() {

            //Arrange
            var primeiraRevogacao = EXPIRA_EM.minusSeconds(60);

            //Act
            token.revogar(primeiraRevogacao);
            token.revogar(EXPIRA_EM.minusSeconds(10));

            //Assert
            assertEquals(primeiraRevogacao, token.getRevogadoEm());
        }
    }
}
