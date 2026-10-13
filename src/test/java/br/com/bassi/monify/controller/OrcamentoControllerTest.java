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

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrcamentoControllerTest {

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
    private String alimentacaoDoBruno;

    @BeforeEach
    void setUp() throws Exception {
        BancoDeTeste.limpar(jdbcTemplate);
        tokenAna = ApiDeTeste.cadastrarELogar(mockMvc, "ana@email.com");
        tokenBruno = ApiDeTeste.cadastrarELogar(mockMvc, "bruno@email.com");
        alimentacaoDaAna = categoriaId(tokenAna, "Alimentação");
        lazerDaAna = categoriaId(tokenAna, "Lazer");
        salarioDaAna = categoriaId(tokenAna, "Salário");
        alimentacaoDoBruno = categoriaId(tokenBruno, "Alimentação");
    }

    private String categoriaId(String token, String nome) throws Exception {
        var resposta = mockMvc.perform(autenticado(get("/api/categorias"), token))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(resposta, "$[?(@.nome == '" + nome + "')].id");
        return ids.getFirst();
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private ResultActions definir(String token, String categoriaId, YearMonth mes, String limite) throws Exception {
        return mockMvc.perform(autenticado(post("/api/orcamentos"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoriaId": "%s", "mesReferencia": "%s", "valorLimite": %s}
                        """.formatted(categoriaId, mes, limite)));
    }

    private String definirId(String token, String categoriaId, YearMonth mes, String limite) throws Exception {
        var resposta = definir(token, categoriaId, mes, limite).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private void registrarDespesa(String categoriaId, String valor, LocalDate data) throws Exception {
        mockMvc.perform(autenticado(post("/api/transacoes"), tokenAna)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoriaId": "%s", "valor": %s, "dataOcorrencia": "%s"}
                        """.formatted(categoriaId, valor, data)));
    }

    // orçamentos passados não podem ser criados pela API: inseridos direto no banco
    private String inserirOrcamentoPassado(String categoriaId, YearMonth mes) {
        var id = java.util.UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO orcamentos (id, usuario_id, categoria_id, mes_referencia, valor_limite)
                SELECT UUID_TO_BIN(?), usuario_id, id, ?, 500.00 FROM categorias WHERE id = UUID_TO_BIN(?)
                """, id, mes.atDay(1), categoriaId);
        return id;
    }

    private ResultActions buscar(String token, String id) throws Exception {
        return mockMvc.perform(autenticado(get("/api/orcamentos/{id}", id), token));
    }

    private ResultActions alterarLimite(String token, String id, String limite) throws Exception {
        return mockMvc.perform(autenticado(put("/api/orcamentos/{id}", id), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"valorLimite": %s}
                        """.formatted(limite)));
    }

    @Nested
    class definir {

        @Test
        @DisplayName("Deve criar o orçamento e retornar 201")
        void deveCriarOrcamento() throws Exception {

            //Act & Assert
            definir(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000")
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.mesReferencia").value(MES_ATUAL.toString()))
                    .andExpect(jsonPath("$.valorLimite").value(1000.00))
                    .andExpect(jsonPath("$.gasto").value(0))
                    .andExpect(jsonPath("$.status").value("DENTRO_DO_LIMITE"))
                    .andExpect(jsonPath("$.categoria.nome").value("Alimentação"));
        }

        @Test
        @DisplayName("Deve atualizar e retornar 200 quando a categoria já tiver orçamento no mês")
        void deveAtualizarQuandoJaExistir() throws Exception {

            //Arrange
            var id = definirId(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");

            //Act & Assert
            definir(tokenAna, alimentacaoDaAna, MES_ATUAL, "1500")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.valorLimite").value(1500.00));
        }

        @Test
        @DisplayName("Deve permitir orçamento para mês futuro")
        void devePermitirMesFuturo() throws Exception {

            //Act & Assert
            definir(tokenAna, alimentacaoDaAna, MES_ATUAL.plusMonths(1), "1000").andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Deve retornar 422 quando o mês for passado")
        void deveRetornar422QuandoMesPassado() throws Exception {

            //Act & Assert
            definir(tokenAna, alimentacaoDaAna, MES_ATUAL.minusMonths(1), "1000")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Orçamento de mês passado não pode ser alterado"));
        }

        @Test
        @DisplayName("Deve retornar 422 quando a categoria for de receita")
        void deveRetornar422QuandoCategoriaDeReceita() throws Exception {

            //Act & Assert
            definir(tokenAna, salarioDaAna, MES_ATUAL, "1000")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("Orçamento só pode ser definido para categorias de despesa"));
        }

        @Test
        @DisplayName("Deve retornar 422 quando a categoria estiver arquivada")
        void deveRetornar422QuandoCategoriaArquivada() throws Exception {

            //Arrange
            mockMvc.perform(autenticado(delete("/api/categorias/{id}", alimentacaoDaAna), tokenAna));

            //Act & Assert
            definir(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000").andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("Deve retornar 404 quando a categoria for de outro usuário")
        void deveRetornar404QuandoCategoriaDeOutroUsuario() throws Exception {

            //Act & Assert
            definir(tokenAna, alimentacaoDoBruno, MES_ATUAL, "1000")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Categoria não encontrada"));
        }

        @Test
        @DisplayName("Deve retornar 400 quando o mês estiver em formato inválido")
        void deveRetornar400QuandoMesInvalido() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(post("/api/orcamentos"), tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"categoriaId": "%s", "mesReferencia": "2026-13", "valorLimite": 100}
                                    """.formatted(alimentacaoDaAna)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class listar {

        @Test
        @DisplayName("Deve calcular gasto, restante, percentual e status com as despesas do mês")
        void deveCalcularNumerosDoMes() throws Exception {

            //Arrange
            definir(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");
            definir(tokenAna, lazerDaAna, MES_ATUAL, "100");
            registrarDespesa(alimentacaoDaAna, "600", HOJE);
            registrarDespesa(alimentacaoDaAna, "250", HOJE.withDayOfMonth(1));
            registrarDespesa(alimentacaoDaAna, "999", MES_ATUAL.minusMonths(1).atDay(15));
            registrarDespesa(lazerDaAna, "150", HOJE);

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/orcamentos"), tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].categoria.nome", contains("Alimentação", "Lazer")))
                    .andExpect(jsonPath("$[0].gasto").value(850.00))
                    .andExpect(jsonPath("$[0].restante").value(150.00))
                    .andExpect(jsonPath("$[0].percentual").value(85.00))
                    .andExpect(jsonPath("$[0].status").value("PROXIMO_DO_LIMITE"))
                    .andExpect(jsonPath("$[1].gasto").value(150.00))
                    .andExpect(jsonPath("$[1].restante").value(-50.00))
                    .andExpect(jsonPath("$[1].percentual").value(150.00))
                    .andExpect(jsonPath("$[1].status").value("ESTOURADO"));
        }

        @Test
        @DisplayName("Deve listar os orçamentos do mês informado")
        void deveListarMesInformado() throws Exception {

            //Arrange
            definir(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");
            definir(tokenAna, lazerDaAna, MES_ATUAL.plusMonths(1), "100");

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/orcamentos"), tokenAna).param("mes", MES_ATUAL.plusMonths(1).toString()))
                    .andExpect(jsonPath("$[*].categoria.nome", contains("Lazer")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando o mês do filtro for inválido")
        void deveRetornar400QuandoMesDoFiltroInvalido() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/orcamentos"), tokenAna).param("mes", "outubro"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'mes'"));
        }

        @Test
        @DisplayName("Deve listar apenas os orçamentos do próprio usuário")
        void deveListarApenasOrcamentosDoUsuario() throws Exception {

            //Arrange
            definir(tokenBruno, alimentacaoDoBruno, MES_ATUAL, "1000");

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/orcamentos"), tokenAna))
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Deve indicar quando a categoria do orçamento estiver arquivada")
        void deveIndicarCategoriaArquivada() throws Exception {

            //Arrange
            definir(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");
            mockMvc.perform(autenticado(delete("/api/categorias/{id}", alimentacaoDaAna), tokenAna));

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/orcamentos"), tokenAna))
                    .andExpect(jsonPath("$[0].categoria.arquivada").value(true));
        }
    }

    @Nested
    class buscar {

        @Test
        @DisplayName("Deve retornar o orçamento com o gasto do mês")
        void deveRetornarOrcamentoComGasto() throws Exception {

            //Arrange
            var id = definirId(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");
            registrarDespesa(alimentacaoDaAna, "100.50", HOJE);

            //Act & Assert
            buscar(tokenAna, id)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gasto").value(100.50))
                    .andExpect(jsonPath("$.restante").value(899.50));
        }

        @Test
        @DisplayName("Deve retornar 404 quando o orçamento for de outro usuário")
        void deveRetornar404QuandoOrcamentoDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = definirId(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");

            //Act & Assert
            buscar(tokenBruno, idDaAna)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Orçamento não encontrado"));
        }
    }

    @Nested
    class alterarLimite {

        @Test
        @DisplayName("Deve alterar o limite e retornar 204")
        void deveAlterarLimite() throws Exception {

            //Arrange
            var id = definirId(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");

            //Act
            alterarLimite(tokenAna, id, "2000").andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id).andExpect(jsonPath("$.valorLimite").value(2000.00));
        }

        @Test
        @DisplayName("Deve retornar 422 quando o orçamento for de mês passado")
        void deveRetornar422QuandoMesPassado() throws Exception {

            //Arrange
            var id = inserirOrcamentoPassado(alimentacaoDaAna, MES_ATUAL.minusMonths(1));

            //Act & Assert
            alterarLimite(tokenAna, id, "2000").andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("Deve retornar 404 e não alterar quando o orçamento for de outro usuário")
        void deveRetornar404QuandoOrcamentoDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = definirId(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");

            //Act
            alterarLimite(tokenBruno, idDaAna, "1").andExpect(status().isNotFound());

            //Assert
            buscar(tokenAna, idDaAna).andExpect(jsonPath("$.valorLimite").value(1000.00));
        }
    }

    @Nested
    class excluir {

        @Test
        @DisplayName("Deve excluir o orçamento e retornar 204")
        void deveExcluirOrcamento() throws Exception {

            //Arrange
            var id = definirId(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");

            //Act
            mockMvc.perform(autenticado(delete("/api/orcamentos/{id}", id), tokenAna))
                    .andExpect(status().isNoContent());

            //Assert
            buscar(tokenAna, id).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 e manter o histórico quando o orçamento for de mês passado")
        void deveRetornar422QuandoMesPassado() throws Exception {

            //Arrange
            var id = inserirOrcamentoPassado(alimentacaoDaAna, MES_ATUAL.minusMonths(1));

            //Act
            mockMvc.perform(autenticado(delete("/api/orcamentos/{id}", id), tokenAna))
                    .andExpect(status().isUnprocessableContent());

            //Assert
            buscar(tokenAna, id).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve retornar 404 e não excluir quando o orçamento for de outro usuário")
        void deveRetornar404QuandoOrcamentoDeOutroUsuario() throws Exception {

            //Arrange
            var idDaAna = definirId(tokenAna, alimentacaoDaAna, MES_ATUAL, "1000");

            //Act
            mockMvc.perform(autenticado(delete("/api/orcamentos/{id}", idDaAna), tokenBruno))
                    .andExpect(status().isNotFound());

            //Assert
            buscar(tokenAna, idDaAna).andExpect(status().isOk());
        }
    }
}
