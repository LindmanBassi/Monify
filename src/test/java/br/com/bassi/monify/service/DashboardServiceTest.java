package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.enums.IconeCategoria;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.repository.TotalMensalPorTipo;
import br.com.bassi.monify.repository.TotalPorCategoria;
import br.com.bassi.monify.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    // 01:30 UTC de 01/11 ainda é 31/10 em São Paulo
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T01:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);
    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);
    private static final YearMonth MAIO = YearMonth.of(2026, 5);

    @Mock
    private TransacaoRepository transacaoRepository;

    private DashboardService dashboardService;

    private record Mensal(Integer getAno, Integer getMes, TipoTransacao getTipo, BigDecimal getTotal)
            implements TotalMensalPorTipo {
    }

    private record PorCategoria(Categoria getCategoria, BigDecimal getTotal) implements TotalPorCategoria {
    }

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(transacaoRepository, CLOCK);
    }

    private static Mensal mensal(YearMonth mes, TipoTransacao tipo, String total) {
        return new Mensal(mes.getYear(), mes.getMonthValue(), tipo, new BigDecimal(total));
    }

    private static PorCategoria porCategoria(String nome, TipoTransacao tipo, String total) {
        return new PorCategoria(new Categoria(USUARIO_ID, nome, tipo, "#FF0000", IconeCategoria.ETIQUETA), new BigDecimal(total));
    }

    private void prepararMeses(List<Mensal> totais) {
        when(transacaoRepository.somarPorMesETipo(USUARIO_ID, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 10, 31)))
                .thenAnswer(inv -> totais);
    }

    private void prepararCategorias(List<PorCategoria> totais) {
        when(transacaoRepository.somarPorCategoria(USUARIO_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenAnswer(inv -> totais);
    }

    @Nested
    class gerar {

        @Test
        @DisplayName("Deve montar seis meses de evolução, preenchendo com zero os meses sem movimento")
        void deveMontarEvolucaoDeSeisMeses() {

            //Arrange
            prepararMeses(List.of(
                    mensal(OUTUBRO, TipoTransacao.RECEITA, "5000.00"),
                    mensal(OUTUBRO, TipoTransacao.DESPESA, "3000.00"),
                    mensal(MAIO, TipoTransacao.DESPESA, "100.00")));
            prepararCategorias(List.of());

            //Act
            var dashboard = dashboardService.gerar(USUARIO_ID, null);

            //Assert
            var evolucao = dashboard.evolucaoMensal();
            assertEquals(OUTUBRO, dashboard.mes());
            assertEquals(6, evolucao.size());
            assertEquals(MAIO, evolucao.getFirst().mes());
            assertEquals(new BigDecimal("-100.00"), evolucao.getFirst().saldo());
            assertEquals(new BigDecimal("0.00"), evolucao.get(1).receitas());
            assertEquals(new BigDecimal("2000.00"), dashboard.resumo().saldo());
        }

        @Test
        @DisplayName("Deve calcular a variação em relação ao mês anterior, usando o valor absoluto para saldo negativo")
        void deveCalcularVariacaoComMesAnterior() {

            //Arrange
            prepararMeses(List.of(
                    mensal(SETEMBRO, TipoTransacao.RECEITA, "4000.00"),
                    mensal(SETEMBRO, TipoTransacao.DESPESA, "4100.00"),
                    mensal(OUTUBRO, TipoTransacao.RECEITA, "5000.00"),
                    mensal(OUTUBRO, TipoTransacao.DESPESA, "4950.00")));
            prepararCategorias(List.of());

            //Act
            var variacao = dashboardService.gerar(USUARIO_ID, OUTUBRO).comparacaoMesAnterior();

            //Assert
            assertEquals(new BigDecimal("25.00"), variacao.receitas());
            assertEquals(new BigDecimal("20.73"), variacao.despesas());
            assertEquals(new BigDecimal("150.00"), variacao.saldo());
        }

        @Test
        @DisplayName("Deve retornar variação nula quando o mês anterior for zero")
        void deveRetornarVariacaoNulaSemBase() {

            //Arrange
            prepararMeses(List.of(mensal(OUTUBRO, TipoTransacao.DESPESA, "300.00")));
            prepararCategorias(List.of());

            //Act
            var variacao = dashboardService.gerar(USUARIO_ID, OUTUBRO).comparacaoMesAnterior();

            //Assert
            assertNull(variacao.receitas());
            assertNull(variacao.despesas());
            assertNull(variacao.saldo());
        }

        @Test
        @DisplayName("Deve separar despesas e receitas por categoria com o percentual de cada uma")
        void deveSepararTotaisPorCategoria() {

            //Arrange
            prepararMeses(List.of(
                    mensal(OUTUBRO, TipoTransacao.RECEITA, "6000.00"),
                    mensal(OUTUBRO, TipoTransacao.DESPESA, "2000.00")));
            prepararCategorias(List.of(
                    porCategoria("Salário", TipoTransacao.RECEITA, "5000.00"),
                    porCategoria("Moradia", TipoTransacao.DESPESA, "1400.00"),
                    porCategoria("Investimentos", TipoTransacao.RECEITA, "1000.00"),
                    porCategoria("Alimentação", TipoTransacao.DESPESA, "600.00")));

            //Act
            var dashboard = dashboardService.gerar(USUARIO_ID, OUTUBRO);

            //Assert
            var gastos = dashboard.gastosPorCategoria();
            var receitas = dashboard.receitasPorCategoria();
            assertEquals(List.of("Moradia", "Alimentação"), gastos.stream().map(g -> g.categoria().nome()).toList());
            assertEquals(new BigDecimal("70.00"), gastos.getFirst().percentual());
            assertEquals(List.of("Salário", "Investimentos"), receitas.stream().map(r -> r.categoria().nome()).toList());
            assertEquals(new BigDecimal("83.33"), receitas.getFirst().percentual());
            assertEquals(new BigDecimal("16.67"), receitas.get(1).percentual());
        }
    }
}
