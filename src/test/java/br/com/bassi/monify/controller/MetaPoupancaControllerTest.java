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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MetaPoupancaControllerTest {

    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

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

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private ResultActions criar(String token, String nome, String alvo, LocalDate prazo) throws Exception {
        var prazoJson = prazo == null ? "null" : "\"" + prazo + "\"";
        return mockMvc.perform(autenticado(post("/api/metas-poupanca"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "%s", "valorAlvo": %s, "prazo": %s}
                        """.formatted(nome, alvo, prazoJson)));
    }

    private String criarId(String nome, String alvo) throws Exception {
        var resposta = criar(tokenAna, nome, alvo, null).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions registrarAporte(String token, String id, String valor, LocalDate data) throws Exception {
        return mockMvc.perform(autenticado(post("/api/metas-poupanca/{id}/aportes", id), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"valor": %s, "data": "%s", "observacao": "Lançamento"}
                        """.formatted(valor, data)));
    }

    private ResultActions buscar(String token, String id) throws Exception {
        return mockMvc.perform(autenticado(get("/api/metas-poupanca/{id}", id), token));
    }

    private ResultActions cancelar(String token, String id) throws Exception {
        return mockMvc.perform(autenticado(patch("/api/metas-poupanca/{id}/cancelar", id), token));
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve criar a meta ativa com o valor a guardar por mês até o prazo")
        void deveCriarMetaComValorPorMes() throws Exception {

            //Act & Assert
            criar(tokenAna, "Viagem", "1200", HOJE.plusMonths(2))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.status").value("ATIVA"))
                    .andExpect(jsonPath("$.acumulado").value(0))
                    .andExpect(jsonPath("$.faltante").value(1200.00))
                    .andExpect(jsonPath("$.valorPorMes").value(400.00))
                    .andExpect(jsonPath("$.prazoVencido").value(false));
        }

        @Test
        @DisplayName("Deve criar meta sem prazo e sem valor por mês")
        void deveCriarMetaSemPrazo() throws Exception {

            //Act & Assert
            criar(tokenAna, "Reserva", "5000", null)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.prazo", nullValue()))
                    .andExpect(jsonPath("$.valorPorMes", nullValue()));
        }

        @Test
        @DisplayName("Deve retornar 422 quando o prazo estiver no passado ou o alvo não for positivo")
        void deveRetornar422QuandoDadosInvalidos() throws Exception {

            //Act & Assert
            criar(tokenAna, "Viagem", "1000", HOJE.minusDays(1))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Prazo não pode estar no passado"));
            criar(tokenAna, "Viagem", "0", null).andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("Deve retornar 400 quando o nome não for informado")
        void deveRetornar400QuandoNomeAusente() throws Exception {

            //Act & Assert
            criar(tokenAna, "", "1000", null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$['campos-invalidos'][0].campo").value("nome"));
        }
    }

    @Nested
    class registrarAporte {

        @Test
        @DisplayName("Deve registrar aportes e resgates atualizando acumulado e percentual")
        void deveRegistrarAportesEResgates() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");

            //Act
            registrarAporte(tokenAna, id, "400", HOJE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.valor").value(400.00))
                    .andExpect(jsonPath("$.observacao").value("Lançamento"));
            registrarAporte(tokenAna, id, "-150", HOJE).andExpect(status().isCreated());

            //Assert
            buscar(tokenAna, id)
                    .andExpect(jsonPath("$.acumulado").value(250.00))
                    .andExpect(jsonPath("$.percentual").value(25.00))
                    .andExpect(jsonPath("$.faltante").value(750.00));
        }

        @Test
        @DisplayName("Deve retornar 422 quando o resgate for maior que o acumulado")
        void deveRetornar422QuandoResgateMaiorQueAcumulado() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");
            registrarAporte(tokenAna, id, "100", HOJE);

            //Act & Assert
            registrarAporte(tokenAna, id, "-100.01", HOJE)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Resgate maior que o valor acumulado"));
        }

        @Test
        @DisplayName("Deve concluir ao atingir o alvo, aceitar resgates e recusar novos aportes")
        void deveConcluirEAceitarApenasResgates() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");

            //Act
            registrarAporte(tokenAna, id, "1000", HOJE);

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.status").value("CONCLUIDA"));
            registrarAporte(tokenAna, id, "-800", HOJE).andExpect(status().isCreated());
            buscar(tokenAna, id)
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.acumulado").value(200.00));
            registrarAporte(tokenAna, id, "50", HOJE)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Meta de poupança concluída aceita apenas resgates"));
        }

        @Test
        @DisplayName("Deve retornar 422 quando a data do aporte for futura")
        void deveRetornar422QuandoDataFutura() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");

            //Act & Assert
            registrarAporte(tokenAna, id, "100", HOJE.plusDays(1)).andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("Deve retornar 404 e não registrar quando a meta for de outro usuário")
        void deveRetornar404QuandoMetaDeOutroUsuario() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");

            //Act
            registrarAporte(tokenBruno, id, "100", HOJE)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Meta de poupança não encontrada"));

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.acumulado").value(0));
        }
    }

    @Nested
    class listarAportes {

        @Test
        @DisplayName("Deve listar o extrato paginado do mais recente para o mais antigo")
        void deveListarExtratoPaginado() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");
            registrarAporte(tokenAna, id, "100", HOJE.minusDays(10));
            registrarAporte(tokenAna, id, "200", HOJE);
            registrarAporte(tokenAna, id, "300", HOJE.minusDays(5));

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/metas-poupanca/{id}/aportes", id), tokenAna).param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo[*].valor", contains(200.00, 300.00)))
                    .andExpect(jsonPath("$.totalElementos").value(3));
        }

        @Test
        @DisplayName("Deve retornar 404 quando a meta for de outro usuário")
        void deveRetornar404QuandoMetaDeOutroUsuario() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/metas-poupanca/{id}/aportes", id), tokenBruno))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class listar {

        @Test
        @DisplayName("Deve listar as metas do usuário com o acumulado e filtrar por status")
        void deveListarEFiltrarPorStatus() throws Exception {

            //Arrange
            var viagem = criarId("Viagem", "1000");
            criarId("Carro", "50000");
            registrarAporte(tokenAna, viagem, "1000", HOJE);

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/metas-poupanca"), tokenAna))
                    .andExpect(jsonPath("$.length()").value(2));
            mockMvc.perform(autenticado(get("/api/metas-poupanca"), tokenAna).param("status", "CONCLUIDA"))
                    .andExpect(jsonPath("$[*].nome", contains("Viagem")))
                    .andExpect(jsonPath("$[0].acumulado").value(1000.00));
        }

        @Test
        @DisplayName("Deve listar apenas as metas do próprio usuário")
        void deveListarApenasMetasDoUsuario() throws Exception {

            //Arrange
            criarId("Viagem", "1000");

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/metas-poupanca"), tokenBruno))
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class editar {

        private ResultActions editarMeta(String token, String id, String nome, String alvo) throws Exception {
            return mockMvc.perform(autenticado(put("/api/metas-poupanca/{id}", id), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"nome": "%s", "valorAlvo": %s, "prazo": null}
                            """.formatted(nome, alvo)));
        }

        @Test
        @DisplayName("Deve editar a meta e concluí-la quando o alvo cair até o acumulado")
        void deveEditarEConcluirQuandoAlvoReduzido() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");
            registrarAporte(tokenAna, id, "600", HOJE);

            //Act
            editarMeta(tokenAna, id, "Viagem curta", "600").andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id)
                    .andExpect(jsonPath("$.nome").value("Viagem curta"))
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando a meta for de outro usuário")
        void deveRetornar404QuandoMetaDeOutroUsuario() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");

            //Act & Assert
            editarMeta(tokenBruno, id, "Invadida", "1").andExpect(status().isNotFound());
        }
    }

    @Nested
    class cancelar {

        @Test
        @DisplayName("Deve cancelar preservando o acumulado e bloquear novos lançamentos")
        void deveCancelarPreservandoAcumulado() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");
            registrarAporte(tokenAna, id, "300", HOJE);

            //Act
            cancelar(tokenAna, id).andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id)
                    .andExpect(jsonPath("$.status").value("CANCELADA"))
                    .andExpect(jsonPath("$.acumulado").value(300.00));
            registrarAporte(tokenAna, id, "-300", HOJE).andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("Deve retornar 422 quando a meta já estiver concluída")
        void deveRetornar422QuandoConcluida() throws Exception {

            //Arrange
            var id = criarId("Viagem", "100");
            registrarAporte(tokenAna, id, "100", HOJE);

            //Act & Assert
            cancelar(tokenAna, id)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Somente metas de poupança ativas podem ser canceladas"));
        }

        @Test
        @DisplayName("Deve retornar 404 e não cancelar quando a meta for de outro usuário")
        void deveRetornar404QuandoMetaDeOutroUsuario() throws Exception {

            //Arrange
            var id = criarId("Viagem", "1000");

            //Act
            cancelar(tokenBruno, id).andExpect(status().isNotFound());

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.status").value("ATIVA"));
        }
    }
}
