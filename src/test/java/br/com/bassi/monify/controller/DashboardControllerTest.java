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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DashboardControllerTest {

    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    private static final YearMonth MES_ATUAL = YearMonth.from(HOJE);

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

        registrar(tokenAna, "Salário", "5000", HOJE);
        registrar(tokenAna, "Investimentos", "1000", MES_ATUAL.atDay(1));
        registrar(tokenAna, "Moradia", "1400", MES_ATUAL.atDay(1));
        registrar(tokenAna, "Alimentação", "600", HOJE);
        registrar(tokenAna, "Salário", "4000", MES_ATUAL.minusMonths(1).atDay(5));
        registrar(tokenAna, "Alimentação", "1600", MES_ATUAL.minusMonths(1).atDay(20));
        registrar(tokenAna, "Alimentação", "300", MES_ATUAL.minusMonths(3).atDay(10));
        registrar(tokenBruno, "Salário", "9999", HOJE);
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private void registrar(String token, String categoria, String valor, LocalDate data) throws Exception {
        var categorias = mockMvc.perform(autenticado(get("/api/categorias"), token))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(categorias, "$[?(@.nome == '" + categoria + "')].id");

        mockMvc.perform(autenticado(post("/api/transacoes"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaId": "%s", "valor": %s, "dataOcorrencia": "%s"}
                                """.formatted(ids.getFirst(), valor, data)))
                .andExpect(status().isCreated());
    }

    @Nested
    class gerar {

        @Test
        @DisplayName("Deve resumir o mês atual com receitas, despesas e saldo")
        void deveResumirMesAtual() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/dashboard"), tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mes").value(MES_ATUAL.toString()))
                    .andExpect(jsonPath("$.resumo.receitas").value(6000.00))
                    .andExpect(jsonPath("$.resumo.despesas").value(2000.00))
                    .andExpect(jsonPath("$.resumo.saldo").value(4000.00));
        }

        @Test
        @DisplayName("Deve comparar com o mês anterior em percentual")
        void deveCompararComMesAnterior() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/dashboard"), tokenAna))
                    .andExpect(jsonPath("$.comparacaoMesAnterior.receitas").value(50.00))
                    .andExpect(jsonPath("$.comparacaoMesAnterior.despesas").value(25.00))
                    .andExpect(jsonPath("$.comparacaoMesAnterior.saldo").value(66.67));
        }

        @Test
        @DisplayName("Deve listar despesas e receitas por categoria, do maior para o menor total")
        void deveListarTotaisPorCategoria() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/dashboard"), tokenAna))
                    .andExpect(jsonPath("$.gastosPorCategoria[*].categoria.nome", contains("Moradia", "Alimentação")))
                    .andExpect(jsonPath("$.gastosPorCategoria[*].percentual", contains(70.00, 30.00)))
                    .andExpect(jsonPath("$.receitasPorCategoria[*].categoria.nome", contains("Salário", "Investimentos")))
                    .andExpect(jsonPath("$.receitasPorCategoria[*].total", contains(5000.00, 1000.00)))
                    .andExpect(jsonPath("$.receitasPorCategoria[0].categoria.cor").exists());
        }

        @Test
        @DisplayName("Deve trazer os seis últimos meses de evolução, com zero nos meses sem movimento")
        void deveTrazerEvolucaoDeSeisMeses() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/dashboard"), tokenAna))
                    .andExpect(jsonPath("$.evolucaoMensal.length()").value(6))
                    .andExpect(jsonPath("$.evolucaoMensal[0].mes").value(MES_ATUAL.minusMonths(5).toString()))
                    .andExpect(jsonPath("$.evolucaoMensal[0].saldo").value(0))
                    .andExpect(jsonPath("$.evolucaoMensal[2].despesas").value(300.00))
                    .andExpect(jsonPath("$.evolucaoMensal[4].saldo").value(2400.00))
                    .andExpect(jsonPath("$.evolucaoMensal[5].mes").value(MES_ATUAL.toString()));
        }

        @Test
        @DisplayName("Deve gerar o dashboard do mês informado, sem variação quando o mês anterior for zero")
        void deveGerarMesInformadoSemBaseDeComparacao() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/dashboard"), tokenAna).param("mes", MES_ATUAL.minusMonths(3).toString()))
                    .andExpect(jsonPath("$.resumo.despesas").value(300.00))
                    .andExpect(jsonPath("$.comparacaoMesAnterior.despesas", nullValue()))
                    .andExpect(jsonPath("$.comparacaoMesAnterior.saldo", nullValue()))
                    .andExpect(jsonPath("$.gastosPorCategoria[0].percentual").value(100.00))
                    .andExpect(jsonPath("$.receitasPorCategoria", empty()));
        }

        @Test
        @DisplayName("Deve considerar apenas as transações do próprio usuário")
        void deveConsiderarApenasDadosDoUsuario() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/dashboard"), tokenBruno))
                    .andExpect(jsonPath("$.resumo.receitas").value(9999.00))
                    .andExpect(jsonPath("$.resumo.despesas").value(0))
                    .andExpect(jsonPath("$.gastosPorCategoria", empty()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando o mês for inválido e 401 sem token")
        void deveRetornarErrosDeEntradaEAutenticacao() throws Exception {

            //Act & Assert
            mockMvc.perform(autenticado(get("/api/dashboard"), tokenAna).param("mes", "outubro"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        }
    }
}
