package br.com.bassi.monify.controller;

import br.com.bassi.monify.TestcontainersConfiguration;
import br.com.bassi.monify.domain.enums.CategoriaPadrao;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenAna;
    private String tokenBruno;

    @BeforeEach
    void setUp() throws Exception {
        BancoDeTeste.limpar(jdbcTemplate);
        tokenAna = ApiDeTeste.cadastrarELogar(mockMvc, "ana@email.com");
        tokenBruno = ApiDeTeste.cadastrarELogar(mockMvc, "bruno@email.com");
    }

    private ResultActions criar(String token, String nome, String tipo, String cor) throws Exception {
        return mockMvc.perform(post("/api/categorias")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "%s", "tipo": "%s", "cor": "%s"}
                        """.formatted(nome, tipo, cor)));
    }

    private String criarId(String token, String nome) throws Exception {
        var resposta = criar(token, nome, "DESPESA", "#123456").andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions editar(String token, String id, String nome, String cor) throws Exception {
        return mockMvc.perform(put("/api/categorias/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "%s", "cor": "%s"}
                        """.formatted(nome, cor)));
    }

    private ResultActions buscar(String token, String id) throws Exception {
        return mockMvc.perform(get("/api/categorias/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions arquivar(String token, String id) throws Exception {
        return mockMvc.perform(delete("/api/categorias/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    @Nested
    class listar {

        @Test
        @DisplayName("Deve listar as categorias padrão do usuário")
        void deveListarCategoriasPadrao() throws Exception {

            //Act & Assert
            mockMvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(CategoriaPadrao.values().length));
        }

        @Test
        @DisplayName("Deve filtrar as categorias pelo tipo")
        void deveFiltrarPorTipo() throws Exception {

            //Act & Assert
            mockMvc.perform(get("/api/categorias").param("tipo", "RECEITA")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[*].tipo", not(hasItem("DESPESA"))));
        }

        @Test
        @DisplayName("Deve ocultar as arquivadas, salvo quando incluirArquivadas for verdadeiro")
        void deveOcultarArquivadas() throws Exception {

            //Arrange
            arquivar(tokenAna, criarId(tokenAna, "Pets"));

            //Act & Assert
            mockMvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAna))
                    .andExpect(jsonPath("$[*].nome", not(hasItem("Pets"))));
            mockMvc.perform(get("/api/categorias").param("incluirArquivadas", "true")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAna))
                    .andExpect(jsonPath("$[*].nome", hasItem("Pets")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando o tipo informado for inválido")
        void deveRetornar400QuandoTipoInvalido() throws Exception {

            //Act & Assert
            mockMvc.perform(get("/api/categorias").param("tipo", "OUTRO")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAna))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'tipo'"));
        }

        @Test
        @DisplayName("Deve listar apenas as categorias do próprio usuário")
        void deveListarApenasCategoriasDoUsuario() throws Exception {

            //Arrange
            criarId(tokenBruno, "Academia");

            //Act & Assert
            mockMvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAna))
                    .andExpect(jsonPath("$[*].nome", not(hasItem("Academia"))));
        }

        @Test
        @DisplayName("Deve retornar 401 quando não houver token")
        void deveRetornar401SemToken() throws Exception {

            //Act & Assert
            mockMvc.perform(get("/api/categorias")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve criar a categoria e retornar 201 com a localização")
        void deveCriarCategoria() throws Exception {

            //Act
            var resposta = criar(tokenAna, "Pets", "DESPESA", "#AABBCC")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value("Pets"))
                    .andExpect(jsonPath("$.tipo").value("DESPESA"))
                    .andExpect(jsonPath("$.arquivada").value(false))
                    .andReturn().getResponse();

            //Assert
            String id = JsonPath.read(resposta.getContentAsString(), "$.id");
            assertEquals("/api/categorias/" + id, resposta.getHeader("Location"));
        }

        @Test
        @DisplayName("Deve retornar 409 quando já existir categoria com o mesmo nome e tipo")
        void deveRetornar409QuandoNomeDuplicado() throws Exception {

            //Act & Assert
            criar(tokenAna, "alimentação", "DESPESA", "#123456")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Já existe uma categoria de despesa com esse nome"));
        }

        @Test
        @DisplayName("Deve permitir o mesmo nome em tipos diferentes")
        void devePermitirMesmoNomeEmTiposDiferentes() throws Exception {

            //Act & Assert
            criar(tokenAna, "Freelance", "RECEITA", "#123456").andExpect(status().isCreated());
            criar(tokenAna, "Freelance", "DESPESA", "#123456").andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Deve permitir que usuários diferentes tenham categorias com o mesmo nome")
        void devePermitirMesmoNomeEntreUsuarios() throws Exception {

            //Act & Assert
            criar(tokenAna, "Pets", "DESPESA", "#123456").andExpect(status().isCreated());
            criar(tokenBruno, "Pets", "DESPESA", "#123456").andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Deve retornar 400 quando a cor não estiver no formato hexadecimal")
        void deveRetornar400QuandoCorInvalida() throws Exception {

            //Act & Assert
            criar(tokenAna, "Pets", "DESPESA", "vermelho")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$['campos-invalidos'][0].campo").value("cor"));
        }

        @Test
        @DisplayName("Deve retornar 400 quando o tipo do corpo for inválido")
        void deveRetornar400QuandoTipoDoCorpoInvalido() throws Exception {

            //Act & Assert
            criar(tokenAna, "Pets", "OUTRO", "#123456")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Requisição inválida"));
        }
    }

    @Nested
    class buscar {

        @Test
        @DisplayName("Deve retornar a categoria do usuário")
        void deveRetornarCategoria() throws Exception {

            //Arrange
            var id = criarId(tokenAna, "Pets");

            //Act & Assert
            buscar(tokenAna, id)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Pets"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando a categoria for de outro usuário")
        void deveRetornar404QuandoCategoriaDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = criarId(tokenAna, "Pets");

            //Act & Assert
            buscar(tokenBruno, idDaAna)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Categoria não encontrada"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando a categoria não existir")
        void deveRetornar404QuandoCategoriaInexistente() throws Exception {

            //Act & Assert
            buscar(tokenAna, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        }
    }

    @Nested
    class editar {

        @Test
        @DisplayName("Deve editar nome e cor e retornar 204")
        void deveEditarCategoria() throws Exception {

            //Arrange
            var id = criarId(tokenAna, "Pets");

            //Act
            editar(tokenAna, id, "Animais", "#000000").andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id)
                    .andExpect(jsonPath("$.nome").value("Animais"))
                    .andExpect(jsonPath("$.cor").value("#000000"))
                    .andExpect(jsonPath("$.tipo").value("DESPESA"));
        }

        @Test
        @DisplayName("Deve permitir editar apenas a cor mantendo o mesmo nome")
        void devePermitirEditarApenasCor() throws Exception {

            //Arrange
            var id = criarId(tokenAna, "Pets");

            //Act & Assert
            editar(tokenAna, id, "Pets", "#000000").andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deve retornar 409 quando outra categoria do mesmo tipo já usar o nome")
        void deveRetornar409QuandoNomeEmUso() throws Exception {

            //Arrange
            var id = criarId(tokenAna, "Pets");

            //Act & Assert
            editar(tokenAna, id, "Lazer", "#000000").andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Deve retornar 404 e não alterar quando a categoria for de outro usuário")
        void deveRetornar404QuandoCategoriaDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = criarId(tokenAna, "Pets");

            //Act
            editar(tokenBruno, idDaAna, "Invadido", "#000000").andExpect(status().isNotFound());

            //Assert
            buscar(tokenAna, idDaAna).andExpect(jsonPath("$.nome").value("Pets"));
        }
    }

    @Nested
    class arquivar {

        @Test
        @DisplayName("Deve arquivar a categoria e retornar 204")
        void deveArquivarCategoria() throws Exception {

            //Arrange
            var id = criarId(tokenAna, "Pets");

            //Act
            arquivar(tokenAna, id).andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.arquivada").value(true));
        }

        @Test
        @DisplayName("Deve retornar 404 e não arquivar quando a categoria for de outro usuário")
        void deveRetornar404QuandoCategoriaDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = criarId(tokenAna, "Pets");

            //Act
            arquivar(tokenBruno, idDaAna).andExpect(status().isNotFound());

            //Assert
            buscar(tokenAna, idDaAna).andExpect(jsonPath("$.arquivada").value(false));
        }

        @Test
        @DisplayName("Deve impedir criar outra categoria com o nome de uma arquivada")
        void deveImpedirRecriarCategoriaArquivada() throws Exception {

            //Arrange
            arquivar(tokenAna, criarId(tokenAna, "Pets"));

            //Act & Assert
            criar(tokenAna, "Pets", "DESPESA", "#123456").andExpect(status().isConflict());
        }
    }

    @Nested
    class desarquivar {

        @Test
        @DisplayName("Deve desarquivar a categoria e retornar 204")
        void deveDesarquivarCategoria() throws Exception {

            //Arrange
            var id = criarId(tokenAna, "Pets");
            arquivar(tokenAna, id);

            //Act
            mockMvc.perform(patch("/api/categorias/{id}/desarquivar", id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAna))
                    .andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.arquivada").value(false));
        }

        @Test
        @DisplayName("Deve retornar 404 quando a categoria for de outro usuário")
        void deveRetornar404QuandoCategoriaDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = criarId(tokenAna, "Pets");
            arquivar(tokenAna, idDaAna);

            //Act & Assert
            mockMvc.perform(patch("/api/categorias/{id}/desarquivar", idDaAna)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBruno))
                    .andExpect(status().isNotFound());
        }
    }
}
