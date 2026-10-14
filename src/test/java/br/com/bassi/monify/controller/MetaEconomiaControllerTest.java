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
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MetaEconomiaControllerTest {

    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    private static final YearMonth MES_ATUAL = YearMonth.from(HOJE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenAna;
    private String tokenBruno;
    private String alimentacaoDaAna;
    private String lazerDaAna;
    private String salarioDaAna;

    @BeforeEach
    void setUp() throws Exception {
        BancoDeTeste.limpar(jdbcTemplate);
        tokenAna = ApiDeTeste.cadastrarELogar(mockMvc, "ana@email.com");
        tokenBruno = ApiDeTeste.cadastrarELogar(mockMvc, "bruno@email.com");
        alimentacaoDaAna = categoriaId("Alimentação");
        lazerDaAna = categoriaId("Lazer");
        salarioDaAna = categoriaId("Salário");
    }

    private String categoriaId(String nome) throws Exception {
        var resposta = mockMvc.perform(autenticado(get("/api/categorias"), tokenAna))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(resposta, "$[?(@.nome == '" + nome + "')].id");
        return ids.getFirst();
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private void registrarDespesa(String categoriaId, String valor, LocalDate data) throws Exception {
        mockMvc.perform(autenticado(post("/api/transacoes"), tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaId": "%s", "valor": %s, "dataOcorrencia": "%s"}
                                """.formatted(categoriaId, valor, data)))
                .andExpect(status().isCreated());
    }

    private ResultActions criar(String token, String categoriaId, YearMonth mes, String tipo, String economia) throws Exception {
        return mockMvc.perform(autenticado(post("/api/metas-economia"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoriaId": "%s", "mesReferencia": "%s", "tipoReferencia": "%s", "valorEconomia": %s}
                        """.formatted(categoriaId, mes, tipo, economia)));
    }

    private String criarId(YearMonth mes, String tipo, String economia) throws Exception {
        var resposta = criar(tokenAna, alimentacaoDaAna, mes, tipo, economia)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions buscar(String token, String id) throws Exception {
        return mockMvc.perform(autenticado(get("/api/metas-economia/{id}", id), token));
    }

    private ResultActions cancelar(String token, String id) throws Exception {
        return mockMvc.perform(autenticado(patch("/api/metas-economia/{id}/cancelar", id), token));
    }

    // simula uma meta criada antes do mês começar, ainda não congelada
    private String inserirMetaNaoCongelada(String categoriaId, YearMonth mes, String economia) {
        var id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO metas_economia (id, usuario_id, categoria_id, mes_referencia, tipo_referencia, valor_economia)
                SELECT UUID_TO_BIN(?), usuario_id, id, ?, 'MES_ANTERIOR', ? FROM categorias WHERE id = UUID_TO_BIN(?)
                """, id, mes.atDay(1), new java.math.BigDecimal(economia), categoriaId);
        return id;
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve criar meta do mês atual já congelada com o gasto do mês anterior")
        void deveCriarMetaCongeladaComMesAnterior() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "1000", MES_ATUAL.minusMonths(1).atDay(10));
            registrarDespesa(alimentacaoDaAna, "300", HOJE);

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL, "MES_ANTERIOR", "200")
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.valorReferencia").value(1000.00))
                    .andExpect(jsonPath("$.valorLimite").value(800.00))
                    .andExpect(jsonPath("$.gasto").value(300.00))
                    .andExpect(jsonPath("$.economiaRealizada").value(700.00))
                    .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                    .andExpect(jsonPath("$.congeladaEm", notNullValue()));
        }

        @Test
        @DisplayName("Deve usar a média dos três meses anteriores contando meses sem gasto como zero")
        void deveUsarMediaDosTresMeses() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "300", MES_ATUAL.minusMonths(3).atDay(5));
            registrarDespesa(alimentacaoDaAna, "600", MES_ATUAL.minusMonths(1).atDay(5));

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL, "MEDIA_TRES_MESES", "100")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.valorReferencia").value(300.00))
                    .andExpect(jsonPath("$.valorLimite").value(200.00));
        }

        @Test
        @DisplayName("Deve usar o limite do orçamento da categoria no mês")
        void deveUsarLimiteDoOrcamento() throws Exception {

            //Arrange
            var resposta = mockMvc.perform(autenticado(post("/api/orcamentos"), tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"categoriaId": "%s", "mesReferencia": "%s", "valorLimite": 1500}
                                    """.formatted(alimentacaoDaAna, MES_ATUAL)))
                    .andReturn().getResponse().getContentAsString();
            String orcamentoId = JsonPath.read(resposta, "$.id");

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL, "ORCAMENTO", "500")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orcamentoId").value(orcamentoId))
                    .andExpect(jsonPath("$.valorReferencia").value(1500.00))
                    .andExpect(jsonPath("$.valorLimite").value(1000.00));
        }

        @Test
        @DisplayName("Deve retornar 422 quando a referência for ORCAMENTO e não houver orçamento")
        void deveRetornar422QuandoSemOrcamento() throws Exception {

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL, "ORCAMENTO", "100")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Não há orçamento para a categoria no mês da meta"));
        }

        @Test
        @DisplayName("Deve retornar 422 quando o histórico for insuficiente para a média de três meses")
        void deveRetornar422QuandoHistoricoInsuficiente() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "600", MES_ATUAL.minusMonths(2).atDay(5));

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL, "MEDIA_TRES_MESES", "100")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Referência indisponível: histórico de transações insuficiente"));
        }

        @Test
        @DisplayName("Deve retornar 422 e não criar quando a economia não deixar limite positivo")
        void deveRetornar422QuandoLimiteInvalido() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", MES_ATUAL.minusMonths(1).atDay(10));

            //Act
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL, "MES_ANTERIOR", "500")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Valor de economia deve ser menor que o valor de referência"));

            //Assert
            mockMvc.perform(autenticado(get("/api/metas-economia"), tokenAna))
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Deve criar meta de mês futuro aguardando o início")
        void deveCriarMetaFutura() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_INICIO"))
                    .andExpect(jsonPath("$.valorReferencia", nullValue()))
                    .andExpect(jsonPath("$.economiaRealizada", nullValue()));
        }

        @Test
        @DisplayName("Deve retornar 409 quando já houver meta ativa para a categoria no mês")
        void deveRetornar409QuandoMetaAtivaExistente() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);
            criarId(MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100");

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "50")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Já existe uma meta de economia ativa para a categoria no mês"));
        }

        @Test
        @DisplayName("Deve retornar 422 quando o mês for passado ou a categoria for de receita")
        void deveRetornar422QuandoMesPassadoOuReceita() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", MES_ATUAL.minusMonths(3).atDay(1));

            //Act & Assert
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL.minusMonths(1), "MES_ANTERIOR", "100")
                    .andExpect(status().isUnprocessableContent());
            criar(tokenAna, salarioDaAna, MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100")
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("Deve retornar 404 quando a categoria for de outro usuário")
        void deveRetornar404QuandoCategoriaDeOutroUsuario() throws Exception {

            //Act & Assert
            criar(tokenBruno, alimentacaoDaAna, MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100")
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class listar {

        @Test
        @DisplayName("Deve congelar ao acessar a meta cujo mês já começou")
        void deveCongelarAoAcessar() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "1000", MES_ATUAL.minusMonths(1).atDay(10));
            registrarDespesa(alimentacaoDaAna, "900", HOJE);
            inserirMetaNaoCongelada(alimentacaoDaAna, MES_ATUAL, "200");

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/metas-economia"), tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].valorReferencia").value(1000.00))
                    .andExpect(jsonPath("$[0].valorLimite").value(800.00))
                    .andExpect(jsonPath("$[0].congeladaEm", notNullValue()))
                    .andExpect(jsonPath("$[0].status").value("NAO_ATINGIDA"));
        }

        @Test
        @DisplayName("Deve cancelar ao acessar a meta cujo limite ficaria inválido no congelamento")
        void deveCancelarAoAcessarQuandoLimiteInvalido() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "100", MES_ATUAL.minusMonths(1).atDay(10));
            inserirMetaNaoCongelada(alimentacaoDaAna, MES_ATUAL, "150");

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/metas-economia"), tokenAna))
                    .andExpect(jsonPath("$[0].status").value("CANCELADA"))
                    .andExpect(jsonPath("$[0].canceladaEm", notNullValue()))
                    .andExpect(jsonPath("$[0].congeladaEm", nullValue()));
        }

        @Test
        @DisplayName("Deve listar apenas as metas do próprio usuário")
        void deveListarApenasMetasDoUsuario() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);
            criarId(MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100");

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/metas-economia"), tokenBruno).param("mes", MES_ATUAL.plusMonths(1).toString()))
                    .andExpect(jsonPath("$.length()").value(0));
            mockMvc.perform(autenticado(get("/api/metas-economia"), tokenAna).param("mes", MES_ATUAL.plusMonths(1).toString()))
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    class buscar {

        @Test
        @DisplayName("Deve retornar 404 quando a meta for de outro usuário")
        void deveRetornar404QuandoMetaDeOutroUsuario() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);
            var id = criarId(MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100");

            //Act & Assert
            buscar(tokenAna, id).andExpect(status().isOk());
            buscar(tokenBruno, id)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Meta de economia não encontrada"));
        }
    }

    @Nested
    class editar {

        private ResultActions editarMeta(String token, String id, String tipo, String economia) throws Exception {
            return mockMvc.perform(autenticado(put("/api/metas-economia/{id}", id), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"tipoReferencia": "%s", "valorEconomia": %s}
                            """.formatted(tipo, economia)));
        }

        @Test
        @DisplayName("Deve editar a meta antes do congelamento")
        void deveEditarAntesDoCongelamento() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);
            var id = criarId(MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100");

            //Act
            editarMeta(tokenAna, id, "MES_ANTERIOR", "250").andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.valorEconomia").value(250.00));
        }

        @Test
        @DisplayName("Deve retornar 422 quando a meta já estiver congelada")
        void deveRetornar422QuandoCongelada() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "1000", MES_ATUAL.minusMonths(1).atDay(10));
            var id = criarId(MES_ATUAL, "MES_ANTERIOR", "100");

            //Act & Assert
            editarMeta(tokenAna, id, "MES_ANTERIOR", "250")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Meta de economia congelada não pode ser editada"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando a meta for de outro usuário")
        void deveRetornar404QuandoMetaDeOutroUsuario() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);
            var id = criarId(MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100");

            //Act & Assert
            editarMeta(tokenBruno, id, "MES_ANTERIOR", "1").andExpect(status().isNotFound());
        }
    }

    @Nested
    class cancelar {

        @Test
        @DisplayName("Deve cancelar a meta pendente e liberar uma nova meta para a categoria no mês")
        void deveCancelarELiberarNovaMeta() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);
            var id = criarId(MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100");

            //Act
            cancelar(tokenAna, id).andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.status").value("CANCELADA"));
            criar(tokenAna, alimentacaoDaAna, MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "50")
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Deve retornar 422 quando a meta não estiver pendente")
        void deveRetornar422QuandoNaoPendente() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);
            var id = criarId(MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100");
            cancelar(tokenAna, id);

            //Act & Assert
            cancelar(tokenAna, id)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Somente metas pendentes podem ser canceladas"));
        }

        @Test
        @DisplayName("Deve retornar 404 e não cancelar quando a meta for de outro usuário")
        void deveRetornar404QuandoMetaDeOutroUsuario() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);
            var id = criarId(MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100");

            //Act
            cancelar(tokenBruno, id).andExpect(status().isNotFound());

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.status").value("AGUARDANDO_INICIO"));
        }
    }

    @Nested
    class cancelamentoAutomatico {

        @Test
        @DisplayName("Deve cancelar as metas pendentes ao arquivar a categoria")
        void deveCancelarAoArquivarCategoria() throws Exception {

            //Arrange
            registrarDespesa(alimentacaoDaAna, "500", HOJE);
            var id = criarId(MES_ATUAL.plusMonths(1), "MES_ANTERIOR", "100");

            //Act
            mockMvc.perform(autenticado(delete("/api/categorias/{id}", alimentacaoDaAna), tokenAna))
                    .andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.status").value("CANCELADA"));
        }

        @Test
        @DisplayName("Deve cancelar a meta vinculada e desvinculá-la ao excluir o orçamento")
        void deveCancelarAoExcluirOrcamento() throws Exception {

            //Arrange
            var resposta = mockMvc.perform(autenticado(post("/api/orcamentos"), tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"categoriaId": "%s", "mesReferencia": "%s", "valorLimite": 1000}
                                    """.formatted(lazerDaAna, MES_ATUAL)))
                    .andReturn().getResponse().getContentAsString();
            String orcamentoId = JsonPath.read(resposta, "$.id");
            var respostaMeta = criar(tokenAna, lazerDaAna, MES_ATUAL, "ORCAMENTO", "200")
                    .andReturn().getResponse().getContentAsString();
            String metaId = JsonPath.read(respostaMeta, "$.id");

            //Act
            mockMvc.perform(autenticado(delete("/api/orcamentos/{id}", orcamentoId), tokenAna))
                    .andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, metaId)
                    .andExpect(jsonPath("$.status").value("CANCELADA"))
                    .andExpect(jsonPath("$.orcamentoId", nullValue()))
                    .andExpect(jsonPath("$.valorReferencia").value(1000.00));
        }
    }
}
