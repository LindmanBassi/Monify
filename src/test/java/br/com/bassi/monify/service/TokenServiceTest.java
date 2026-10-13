package br.com.bassi.monify.service;

import br.com.bassi.monify.config.PropriedadesAutenticacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-12T15:00:00Z");
    private static final UUID USUARIO_ID = UUID.randomUUID();

    private final SecretKeySpec chave = new SecretKeySpec(
            "segredo-de-teste-com-pelo-menos-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final PropriedadesAutenticacao propriedades = new PropriedadesAutenticacao(
            "nao-utilizado", Duration.ofMinutes(15), Duration.ofDays(7));

    private final TokenService tokenService = new TokenService(
            NimbusJwtEncoder.withSecretKey(chave).algorithm(MacAlgorithm.HS256).build(),
            propriedades,
            Clock.fixed(AGORA, ZoneOffset.UTC));

    @Nested
    class gerarTokenAcesso {

        @Test
        @DisplayName("Deve gerar JWT assinado com o ID do usuário, o emissor e a expiração configurada")
        void deveGerarJwtComUsuarioEmissorEExpiracao() {

            //Arrange
            var decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
            decoder.setJwtValidator(new JwtIssuerValidator(TokenService.EMISSOR));

            //Act
            var jwt = decoder.decode(tokenService.gerarTokenAcesso(USUARIO_ID));

            //Assert
            assertEquals(USUARIO_ID.toString(), jwt.getSubject());
            assertEquals(AGORA, jwt.getIssuedAt());
            assertEquals(AGORA.plus(Duration.ofMinutes(15)), jwt.getExpiresAt());
            assertEquals("HS256", jwt.getHeaders().get("alg"));
        }
    }

    @Nested
    class gerarTokenRenovacao {

        @Test
        @DisplayName("Deve gerar tokens aleatórios e diferentes a cada chamada")
        void deveGerarTokensAleatorios() {

            //Act
            var primeiro = tokenService.gerarTokenRenovacao();
            var segundo = tokenService.gerarTokenRenovacao();

            //Assert
            assertNotEquals(primeiro, segundo);
            assertEquals(43, primeiro.length());
        }
    }

    @Nested
    class calcularHash {

        @Test
        @DisplayName("Deve gerar o mesmo hash SHA-256 em hexadecimal para o mesmo token")
        void deveGerarHashDeterministico() {

            //Act
            var hash = tokenService.calcularHash("token");

            //Assert
            assertEquals(hash, tokenService.calcularHash("token"));
            assertEquals(64, hash.length());
            assertNotEquals(hash, tokenService.calcularHash("outro-token"));
        }
    }

    @Nested
    class calcularExpiracaoTokenRenovacao {

        @Test
        @DisplayName("Deve expirar após a duração configurada do token de renovação")
        void deveExpirarAposDuracaoConfigurada() {

            //Act & Assert
            assertEquals(AGORA.plus(Duration.ofDays(7)), tokenService.calcularExpiracaoTokenRenovacao());
        }
    }
}
