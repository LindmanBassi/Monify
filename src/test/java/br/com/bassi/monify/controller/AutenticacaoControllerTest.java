package br.com.bassi.monify.controller;

import br.com.bassi.monify.TestcontainersConfiguration;
import br.com.bassi.monify.domain.enums.CategoriaPadrao;
import br.com.bassi.monify.repository.CategoriaRepository;
import br.com.bassi.monify.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AutenticacaoControllerTest {

    private static final String EMAIL = "henrique@email.com";
    private static final String SENHA = "senha-forte";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @BeforeEach
    void setUp() {
        BancoDeTeste.limpar(jdbcTemplate);
    }

    private ResultActions cadastrar(String nome, String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "%s", "email": "%s", "senha": "%s"}
                        """.formatted(nome, email, senha)));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "senha": "%s"}
                        """.formatted(email, senha)));
    }

    private ResultActions renovar(String tokenRenovacao) throws Exception {
        return mockMvc.perform(post("/api/auth/renovar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tokenRenovacao": "%s"}
                        """.formatted(tokenRenovacao)));
    }

    private String tokenRenovacaoDoLogin() throws Exception {
        cadastrar("Henrique", EMAIL, SENHA);
        var resposta = login(EMAIL, SENHA).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.tokenRenovacao");
    }

    @Nested
    class cadastrar {

        @Test
        @DisplayName("Deve cadastrar o usuário com senha criptografada e categorias padrão")
        void deveCadastrarUsuario() throws Exception {

            //Act
            var resposta = cadastrar("Henrique", "Henrique@Email.com", SENHA)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/usuarios/me"))
                    .andExpect(jsonPath("$.nome").value("Henrique"))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.senha").doesNotExist())
                    .andReturn().getResponse().getContentAsString();

            //Assert
            var usuarioId = UUID.fromString(JsonPath.read(resposta, "$.id"));
            var usuario = usuarioRepository.findById(usuarioId).orElseThrow();
            assertNotEquals(SENHA, usuario.getSenhaHash());
            assertTrue(usuario.getSenhaHash().startsWith("$2a$"));
            assertEquals(CategoriaPadrao.values().length, categoriaRepository.findAllByUsuarioId(usuarioId).size());
        }

        @Test
        @DisplayName("Deve retornar 409 quando o e-mail já estiver cadastrado, ignorando maiúsculas")
        void deveRetornar409QuandoEmailJaCadastrado() throws Exception {

            //Arrange
            cadastrar("Henrique", EMAIL, SENHA);

            //Act & Assert
            cadastrar("Outro", "HENRIQUE@email.com", SENHA)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("E-mail já cadastrado"));
        }

        @Test
        @DisplayName("Deve retornar 400 quando os campos forem inválidos")
        void deveRetornar400QuandoCamposInvalidos() throws Exception {

            //Act & Assert
            cadastrar("", "email-invalido", "curta")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$['campos-invalidos'].length()").value(3));
        }
    }

    @Nested
    class login {

        @Test
        @DisplayName("Deve retornar os tokens quando as credenciais forem válidas")
        void deveRetornarTokensQuandoCredenciaisValidas() throws Exception {

            //Arrange
            cadastrar("Henrique", EMAIL, SENHA);

            //Act & Assert
            login(EMAIL, SENHA)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenAcesso").isNotEmpty())
                    .andExpect(jsonPath("$.tokenRenovacao").isNotEmpty())
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    .andExpect(jsonPath("$.expiraEmSegundos").value(900));
        }

        @Test
        @DisplayName("Deve retornar 401 com mensagem genérica quando a senha estiver errada")
        void deveRetornar401QuandoSenhaErrada() throws Exception {

            //Arrange
            cadastrar("Henrique", EMAIL, SENHA);

            //Act & Assert
            login(EMAIL, "senha-errada")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Credenciais inválidas"));
        }

        @Test
        @DisplayName("Deve retornar 401 com a mesma mensagem quando o e-mail não existir")
        void deveRetornar401QuandoEmailNaoExistir() throws Exception {

            //Act & Assert
            login("ninguem@email.com", SENHA)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Credenciais inválidas"));
        }
    }

    @Nested
    class renovar {

        @Test
        @DisplayName("Deve emitir um novo par de tokens e invalidar o token usado")
        void deveRotacionarTokens() throws Exception {

            //Arrange
            var tokenUsado = tokenRenovacaoDoLogin();

            //Act
            var resposta = renovar(tokenUsado)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            //Assert
            String novoToken = JsonPath.read(resposta, "$.tokenRenovacao");
            assertNotEquals(tokenUsado, novoToken);
            renovar(novoToken).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve revogar todos os tokens do usuário quando um token já usado for reutilizado")
        void deveRevogarTodosOsTokensQuandoTokenReutilizado() throws Exception {

            //Arrange
            var tokenUsado = tokenRenovacaoDoLogin();
            var resposta = renovar(tokenUsado).andReturn().getResponse().getContentAsString();
            String novoToken = JsonPath.read(resposta, "$.tokenRenovacao");

            //Act
            renovar(tokenUsado)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Sessão encerrada por segurança. Faça login novamente"));

            //Assert
            renovar(novoToken).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve retornar 401 quando o token de renovação não existir")
        void deveRetornar401QuandoTokenNaoExistir() throws Exception {

            //Act & Assert
            renovar("token-inexistente")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Token de renovação inválido"));
        }
    }

    @Nested
    class logout {

        @Test
        @DisplayName("Deve revogar o token de renovação e impedir novas renovações com ele")
        void deveRevogarTokenNoLogout() throws Exception {

            //Arrange
            var tokenRenovacao = tokenRenovacaoDoLogin();

            //Act
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tokenRenovacao": "%s"}
                                    """.formatted(tokenRenovacao)))
                    .andExpect(status().isNoContent());

            //Assert
            renovar(tokenRenovacao).andExpect(status().isUnauthorized());
        }
    }
}
