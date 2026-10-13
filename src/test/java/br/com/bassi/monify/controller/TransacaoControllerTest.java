package br.com.bassi.monify.controller;

import br.com.bassi.monify.TestcontainersConfiguration;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TransacaoControllerTest {

    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenAna;
    private String tokenBruno;
    private String alimentacaoDaAna;
    private String salarioDaAna;
    private String alimentacaoDoBruno;

    @BeforeEach
    void setUp() throws Exception {
        BancoDeTeste.limpar(jdbcTemplate);
        tokenAna = ApiDeTeste.cadastrarELogar(mockMvc, "ana@email.com");
        tokenBruno = ApiDeTeste.cadastrarELogar(mockMvc, "bruno@email.com");
        alimentacaoDaAna = categoriaId(tokenAna, "Alimentação");
        salarioDaAna = categoriaId(tokenAna, "Salário");
        alimentacaoDoBruno = categoriaId(tokenBruno, "Alimentação");
    }

    private String categoriaId(String token, String nome) throws Exception {
        var resposta = mockMvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(resposta, "$[?(@.nome == '" + nome + "')].id");
        return ids.getFirst();
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private ResultActions criar(String token, String categoriaId, String valor, String descricao, LocalDate data) throws Exception {
        return mockMvc.perform(autenticado(post("/api/transacoes"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoriaId": "%s", "valor": %s, "descricao": "%s", "dataOcorrencia": "%s"}
                        """.formatted(categoriaId, valor, descricao, data)));
    }

    private String criarId(String token, String categoriaId, String valor, String descricao, LocalDate data) throws Exception {
        var resposta = criar(token, categoriaId, valor, descricao, data).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions editar(String token, String id, String categoriaId, String valor, LocalDate data) throws Exception {
        return mockMvc.perform(autenticado(put("/api/transacoes/{id}", id), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoriaId": "%s", "valor": %s, "descricao": "Editada", "dataOcorrencia": "%s"}
                        """.formatted(categoriaId, valor, data)));
    }

    private ResultActions buscar(String token, String id) throws Exception {
        return mockMvc.perform(autenticado(get("/api/transacoes/{id}", id), token));
    }

    private ResultActions listar(String token, String... parametros) throws Exception {
        var request = autenticado(get("/api/transacoes"), token);
        for (var i = 0; i < parametros.length; i += 2) {
            request.param(parametros[i], parametros[i + 1]);
        }
        return mockMvc.perform(request);
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve registrar a transação com o tipo da categoria e retornar 201")
        void deveRegistrarTransacao() throws Exception {

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, "52.9", "Feira", HOJE)
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.tipo").value("DESPESA"))
                    .andExpect(jsonPath("$.valor").value(52.90))
                    .andExpect(jsonPath("$.categoria.nome").value("Alimentação"))
                    .andExpect(jsonPath("$.dataOcorrencia").value(HOJE.toString()));
        }

        @Test
        @DisplayName("Deve retornar 404 quando a categoria for de outro usuário")
        void deveRetornar404QuandoCategoriaDeOutroUsuario() throws Exception {

            //Act & Assert
            criar(tokenAna, alimentacaoDoBruno, "10", "Feira", HOJE)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Categoria não encontrada"));
        }

        @Test
        @DisplayName("Deve retornar 422 quando a data for futura")
        void deveRetornar422QuandoDataFutura() throws Exception {

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE.plusDays(1))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Data da transação não pode ser futura"));
        }

        @Test
        @DisplayName("Deve retornar 422 quando a categoria estiver arquivada")
        void deveRetornar422QuandoCategoriaArquivada() throws Exception {

            //Arrange
            mockMvc.perform(autenticado(delete("/api/categorias/{id}", alimentacaoDaAna), tokenAna));

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Categoria arquivada não pode ser utilizada"));
        }

        @Test
        @DisplayName("Deve retornar 422 quando o valor não for positivo ou tiver mais de duas casas")
        void deveRetornar422QuandoValorInvalido() throws Exception {

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, "0", "Feira", HOJE).andExpect(status().isUnprocessableContent());
            criar(tokenAna, alimentacaoDaAna, "10.999", "Feira", HOJE).andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("Deve retornar 400 quando os campos obrigatórios estiverem ausentes")
        void deveRetornar400QuandoCamposAusentes() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(post("/api/transacoes"), tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$['campos-invalidos'].length()").value(3));
        }
    }

    @Nested
    class listar {

        @Test
        @DisplayName("Deve listar paginado da data mais recente para a mais antiga")
        void deveListarPaginadoEOrdenado() throws Exception {

            //Arrange
            criar(tokenAna, alimentacaoDaAna, "10", "Antiga", HOJE.minusDays(10));
            criar(tokenAna, alimentacaoDaAna, "20", "Recente", HOJE);
            criar(tokenAna, alimentacaoDaAna, "30", "Meio", HOJE.minusDays(5));

            //Act & Assert
            listar(tokenAna, "size", "2")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo[*].descricao", contains("Recente", "Meio")))
                    .andExpect(jsonPath("$.pagina").value(0))
                    .andExpect(jsonPath("$.tamanho").value(2))
                    .andExpect(jsonPath("$.totalElementos").value(3))
                    .andExpect(jsonPath("$.totalPaginas").value(2));
        }

        @Test
        @DisplayName("Deve filtrar por período")
        void deveFiltrarPorPeriodo() throws Exception {

            //Arrange
            criar(tokenAna, alimentacaoDaAna, "10", "Antiga", HOJE.minusDays(10));
            criar(tokenAna, alimentacaoDaAna, "20", "Recente", HOJE);

            //Act & Assert
            listar(tokenAna, "inicio", HOJE.minusDays(1).toString(), "fim", HOJE.toString())
                    .andExpect(jsonPath("$.conteudo[*].descricao", contains("Recente")));
        }

        @Test
        @DisplayName("Deve filtrar por categoria e por tipo")
        void deveFiltrarPorCategoriaETipo() throws Exception {

            //Arrange
            criar(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE);
            criar(tokenAna, salarioDaAna, "5000", "Salário de outubro", HOJE);

            //Act & Assert
            listar(tokenAna, "categoriaId", salarioDaAna)
                    .andExpect(jsonPath("$.conteudo[*].descricao", contains("Salário de outubro")));
            listar(tokenAna, "tipo", "DESPESA")
                    .andExpect(jsonPath("$.conteudo[*].descricao", contains("Feira")));
        }

        @Test
        @DisplayName("Deve buscar texto na descrição sem diferenciar maiúsculas")
        void deveBuscarTextoNaDescricao() throws Exception {

            //Arrange
            criar(tokenAna, alimentacaoDaAna, "10", "Feira do bairro", HOJE);
            criar(tokenAna, alimentacaoDaAna, "20", "Restaurante", HOJE);

            //Act & Assert
            listar(tokenAna, "texto", "FEIRA")
                    .andExpect(jsonPath("$.conteudo[*].descricao", contains("Feira do bairro")));
        }

        @Test
        @DisplayName("Deve tratar curingas do texto como caracteres comuns")
        void deveTratarCuringasComoTexto() throws Exception {

            //Arrange
            criar(tokenAna, alimentacaoDaAna, "10", "Desconto de 50%", HOJE);
            criar(tokenAna, alimentacaoDaAna, "20", "Feira", HOJE);

            //Act & Assert
            listar(tokenAna, "texto", "%")
                    .andExpect(jsonPath("$.conteudo[*].descricao", contains("Desconto de 50%")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando o início for posterior ao fim")
        void deveRetornar400QuandoPeriodoInvalido() throws Exception {

            //Act & Assert
            listar(tokenAna, "inicio", HOJE.toString(), "fim", HOJE.minusDays(1).toString())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$['campos-invalidos'][0].mensagem")
                            .value("Data de início não pode ser posterior à data de fim"));
        }

        @Test
        @DisplayName("Deve retornar 400 quando a data do filtro for inválida")
        void deveRetornar400QuandoDataInvalida() throws Exception {

            //Act & Assert
            listar(tokenAna, "inicio", "ontem")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$['campos-invalidos'][0].campo").value("inicio"))
                    .andExpect(jsonPath("$['campos-invalidos'][0].mensagem").value("Valor inválido"));
        }

        @Test
        @DisplayName("Deve limitar o tamanho da página a 100 itens")
        void deveLimitarTamanhoDaPagina() throws Exception {

            //Act & Assert
            listar(tokenAna, "size", "1000").andExpect(jsonPath("$.tamanho").value(100));
        }

        @Test
        @DisplayName("Deve listar apenas as transações do próprio usuário")
        void deveListarApenasTransacoesDoUsuario() throws Exception {

            //Arrange
            criar(tokenAna, alimentacaoDaAna, "10", "Da Ana", HOJE);
            criar(tokenBruno, alimentacaoDoBruno, "20", "Do Bruno", HOJE);

            //Act & Assert
            listar(tokenAna)
                    .andExpect(jsonPath("$.totalElementos").value(1))
                    .andExpect(jsonPath("$.conteudo[*].descricao", not(hasItem("Do Bruno"))));
        }
    }

    @Nested
    class buscar {

        @Test
        @DisplayName("Deve retornar 404 quando a transação for de outro usuário")
        void deveRetornar404QuandoTransacaoDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = criarId(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE);

            //Act & Assert
            buscar(tokenAna, idDaAna).andExpect(status().isOk());
            buscar(tokenBruno, idDaAna)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Transação não encontrada"));
        }
    }

    @Nested
    class editar {

        @Test
        @DisplayName("Deve editar a transação e atualizar o tipo pela nova categoria")
        void deveEditarTransacao() throws Exception {

            //Arrange
            var id = criarId(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE);

            //Act
            editar(tokenAna, id, salarioDaAna, "5000", HOJE).andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id)
                    .andExpect(jsonPath("$.tipo").value("RECEITA"))
                    .andExpect(jsonPath("$.valor").value(5000.00))
                    .andExpect(jsonPath("$.descricao").value("Editada"));
        }

        @Test
        @DisplayName("Deve permitir editar transação de categoria arquivada mantendo a categoria")
        void devePermitirEditarComCategoriaArquivadaMantida() throws Exception {

            //Arrange
            var id = criarId(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE);
            mockMvc.perform(autenticado(delete("/api/categorias/{id}", alimentacaoDaAna), tokenAna));

            //Act & Assert
            editar(tokenAna, id, alimentacaoDaAna, "15", HOJE).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deve retornar 422 quando a transação for movida para categoria arquivada")
        void deveRetornar422QuandoMovidaParaCategoriaArquivada() throws Exception {

            //Arrange
            var id = criarId(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE);
            mockMvc.perform(autenticado(delete("/api/categorias/{id}", salarioDaAna), tokenAna));

            //Act & Assert
            editar(tokenAna, id, salarioDaAna, "10", HOJE).andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("Deve retornar 404 e não alterar quando a transação for de outro usuário")
        void deveRetornar404QuandoTransacaoDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = criarId(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE);

            //Act
            editar(tokenBruno, idDaAna, alimentacaoDoBruno, "999", HOJE).andExpect(status().isNotFound());

            //Assert
            buscar(tokenAna, idDaAna).andExpect(jsonPath("$.valor").value(10.00));
        }
    }

    @Nested
    class excluir {

        @Test
        @DisplayName("Deve excluir definitivamente a transação")
        void deveExcluirTransacao() throws Exception {

            //Arrange
            var id = criarId(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE);

            //Act
            mockMvc.perform(autenticado(delete("/api/transacoes/{id}", id), tokenAna))
                    .andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 404 e não excluir quando a transação for de outro usuário")
        void deveRetornar404QuandoTransacaoDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = criarId(tokenAna, alimentacaoDaAna, "10", "Feira", HOJE);

            //Act
            mockMvc.perform(autenticado(delete("/api/transacoes/{id}", idDaAna), tokenBruno))
                    .andExpect(status().isNotFound());

            //Assert
            buscar(tokenAna, idDaAna).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve retornar 404 quando a transação não existir")
        void deveRetornar404QuandoTransacaoInexistente() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(delete("/api/transacoes/{id}", UUID.randomUUID()), tokenAna))
                    .andExpect(status().isNotFound());
        }
    }
}
