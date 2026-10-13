package br.com.bassi.monify.controller;

import br.com.bassi.monify.TestcontainersConfiguration;
import br.com.bassi.monify.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void setUp() {
        BancoDeTeste.limpar(jdbcTemplate);
    }

    private ResultActions buscarAutenticado(String tokenAcesso) throws Exception {
        return mockMvc.perform(get("/api/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAcesso));
    }

    private JwtClaimsSet claims(Instant emitidoEm, Duration duracao) {
        return JwtClaimsSet.builder()
                .issuer(TokenService.EMISSOR)
                .subject(UUID.randomUUID().toString())
                .issuedAt(emitidoEm)
                .expiresAt(emitidoEm.plus(duracao))
                .build();
    }

    @Nested
    class buscarAutenticado {

        @Test
        @DisplayName("Deve retornar o usuário dono do token de acesso")
        void deveRetornarUsuarioDoToken() throws Exception {

            //Arrange
            var tokenAcesso = ApiDeTeste.cadastrarELogar(mockMvc, "henrique@email.com");

            //Act & Assert
            buscarAutenticado(tokenAcesso)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("henrique@email.com"));
        }

        @Test
        @DisplayName("Deve retornar os dados de cada usuário de acordo com o próprio token")
        void deveIsolarUsuariosPeloToken() throws Exception {

            //Arrange
            var tokenAna = ApiDeTeste.cadastrarELogar(mockMvc, "ana@email.com");
            var tokenBruno = ApiDeTeste.cadastrarELogar(mockMvc, "bruno@email.com");

            //Act & Assert
            buscarAutenticado(tokenAna).andExpect(jsonPath("$.email").value("ana@email.com"));
            buscarAutenticado(tokenBruno).andExpect(jsonPath("$.email").value("bruno@email.com"));
        }

        @Test
        @DisplayName("Deve retornar 401 com ProblemDetail quando não houver token")
        void deveRetornar401QuandoSemToken() throws Exception {

            //Act & Assert
            mockMvc.perform(get("/api/usuarios/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(jsonPath("$.title").value("Não autenticado"));
        }

        @Test
        @DisplayName("Deve retornar 401 quando o token for malformado")
        void deveRetornar401QuandoTokenMalformado() throws Exception {

            //Act & Assert
            buscarAutenticado("token.invalido.qualquer")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Não autenticado"));
        }

        @Test
        @DisplayName("Deve retornar 401 quando o token estiver expirado")
        void deveRetornar401QuandoTokenExpirado() throws Exception {

            //Arrange
            var cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
            var claims = claims(Instant.now().minus(Duration.ofHours(1)), Duration.ofMinutes(15));
            var tokenExpirado = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();

            //Act & Assert
            buscarAutenticado(tokenExpirado).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve retornar 401 quando o token for assinado com outro segredo")
        void deveRetornar401QuandoAssinaturaInvalida() throws Exception {

            //Arrange
            var outroSegredo = new SecretKeySpec(
                    "outro-segredo-com-pelo-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            var outroEncoder = NimbusJwtEncoder.withSecretKey(outroSegredo).algorithm(MacAlgorithm.HS256).build();
            var cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
            var tokenForjado = outroEncoder.encode(JwtEncoderParameters.from(cabecalho, claims(Instant.now(), Duration.ofMinutes(15))))
                    .getTokenValue();

            //Act & Assert
            buscarAutenticado(tokenForjado).andExpect(status().isUnauthorized());
        }
    }
}
