package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.StatusMetaPoupanca;
import br.com.bassi.monify.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MetaPoupancaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 9);

    private final MetaPoupanca viagem = new MetaPoupanca(UUID.randomUUID(), " Viagem ", new BigDecimal("1000"), null, HOJE);

    private MetaPoupanca comPrazo(LocalDate prazo) {
        return new MetaPoupanca(UUID.randomUUID(), "Viagem", new BigDecimal("1000"), prazo, HOJE);
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve começar ativa e sem valor acumulado")
        void deveComecarAtivaESemAcumulado() {

            //Act & Assert
            assertEquals("Viagem", viagem.getNome());
            assertEquals(StatusMetaPoupanca.ATIVA, viagem.getStatus());
            assertEquals(new BigDecimal("0.00"), viagem.calcularAcumulado());
            assertEquals(new BigDecimal("1000.00"), viagem.calcularFaltante());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o valor alvo não for positivo")
        void deveLancarExcecaoQuandoValorAlvoNaoPositivo() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> new MetaPoupanca(UUID.randomUUID(), "X", BigDecimal.ZERO, null, HOJE));
        }

        @Test
        @DisplayName("Deve lançar exceção quando o prazo estiver no passado")
        void deveLancarExcecaoQuandoPrazoNoPassado() {

            //Act & Assert
            var excecao = assertThrows(RegraNegocioException.class, () -> comPrazo(HOJE.minusDays(1)));
            assertEquals("Prazo não pode estar no passado", excecao.getMessage());
            assertDoesNotThrow(() -> comPrazo(HOJE));
        }
    }

    @Nested
    class editar {

        @Test
        @DisplayName("Deve editar nome, alvo e prazo da meta ativa")
        void deveEditarMetaAtiva() {

            //Act
            viagem.editar("Viagem ao Japão", new BigDecimal("5000"), HOJE.plusMonths(6), HOJE);

            //Assert
            assertEquals("Viagem ao Japão", viagem.getNome());
            assertEquals(new BigDecimal("5000.00"), viagem.getValorAlvo());
            assertEquals(HOJE.plusMonths(6), viagem.getPrazo());
        }

        @Test
        @DisplayName("Deve concluir a meta quando o novo alvo for menor ou igual ao acumulado")
        void deveConcluirQuandoAlvoReduzidoAteAcumulado() {

            //Arrange
            viagem.registrarAporte(new BigDecimal("600"), HOJE, null, HOJE);

            //Act
            viagem.editar("Viagem", new BigDecimal("600"), null, HOJE);

            //Assert
            assertEquals(StatusMetaPoupanca.CONCLUIDA, viagem.getStatus());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a meta não estiver ativa")
        void deveLancarExcecaoQuandoNaoAtiva() {

            //Arrange
            viagem.cancelar();

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> viagem.editar("Viagem", new BigDecimal("2000"), null, HOJE));
        }
    }

    @Nested
    class registrarAporte {

        @Test
        @DisplayName("Deve acumular aportes e resgates")
        void deveAcumularAportesEResgates() {

            //Act
            var aporte = viagem.registrarAporte(new BigDecimal("300"), HOJE, " Salário ", HOJE);
            viagem.registrarAporte(new BigDecimal("-100"), HOJE, "  ", HOJE);

            //Assert
            assertSame(viagem, aporte.getMetaPoupanca());
            assertEquals("Salário", aporte.getObservacao());
            assertNull(viagem.getAportes().get(1).getObservacao());
            assertEquals(new BigDecimal("200.00"), viagem.calcularAcumulado());
            assertEquals(new BigDecimal("800.00"), viagem.calcularFaltante());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o resgate deixar o acumulado negativo")
        void deveLancarExcecaoQuandoAcumuladoFicarNegativo() {

            //Arrange
            viagem.registrarAporte(new BigDecimal("100"), HOJE, null, HOJE);

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> viagem.registrarAporte(new BigDecimal("-100.01"), HOJE, null, HOJE));
            assertEquals(1, viagem.getAportes().size());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o valor for zero ou a data for futura")
        void deveLancarExcecaoQuandoValorZeroOuDataFutura() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> viagem.registrarAporte(BigDecimal.ZERO, HOJE, null, HOJE));
            var excecao = assertThrows(RegraNegocioException.class,
                    () -> viagem.registrarAporte(BigDecimal.TEN, HOJE.plusDays(1), null, HOJE));
            assertEquals("Data do aporte não pode ser futura", excecao.getMessage());
        }

        @Test
        @DisplayName("Deve concluir a meta ao atingir o alvo")
        void deveConcluirAoAtingirAlvo() {

            //Act
            viagem.registrarAporte(new BigDecimal("1000"), HOJE, null, HOJE);

            //Assert
            assertEquals(StatusMetaPoupanca.CONCLUIDA, viagem.getStatus());
            assertEquals(new BigDecimal("0.00"), viagem.calcularFaltante());
        }

        @Test
        @DisplayName("Deve aceitar resgate na meta concluída mantendo-a concluída")
        void deveAceitarResgateNaMetaConcluida() {

            //Arrange
            viagem.registrarAporte(new BigDecimal("1000"), HOJE, null, HOJE);

            //Act
            viagem.registrarAporte(new BigDecimal("-800"), HOJE, "Passagens", HOJE);

            //Assert
            assertEquals(StatusMetaPoupanca.CONCLUIDA, viagem.getStatus());
            assertEquals(new BigDecimal("200.00"), viagem.calcularAcumulado());
        }

        @Test
        @DisplayName("Deve lançar exceção ao aportar em meta concluída")
        void deveLancarExcecaoAoAportarEmMetaConcluida() {

            //Arrange
            viagem.registrarAporte(new BigDecimal("1000"), HOJE, null, HOJE);

            //Act & Assert
            var excecao = assertThrows(RegraNegocioException.class,
                    () -> viagem.registrarAporte(new BigDecimal("10"), HOJE, null, HOJE));
            assertEquals("Meta de poupança concluída aceita apenas resgates", excecao.getMessage());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a meta estiver cancelada")
        void deveLancarExcecaoQuandoCancelada() {

            //Arrange
            viagem.registrarAporte(new BigDecimal("100"), HOJE, null, HOJE);
            viagem.cancelar();

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> viagem.registrarAporte(new BigDecimal("-100"), HOJE, null, HOJE));
        }
    }

    @Nested
    class cancelar {

        @Test
        @DisplayName("Deve cancelar a meta ativa preservando o acumulado")
        void deveCancelarPreservandoAcumulado() {

            //Arrange
            viagem.registrarAporte(new BigDecimal("300"), HOJE, null, HOJE);

            //Act
            viagem.cancelar();

            //Assert
            assertEquals(StatusMetaPoupanca.CANCELADA, viagem.getStatus());
            assertEquals(new BigDecimal("300.00"), viagem.calcularAcumulado());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a meta já estiver concluída")
        void deveLancarExcecaoQuandoConcluida() {

            //Arrange
            viagem.registrarAporte(new BigDecimal("1000"), HOJE, null, HOJE);

            //Act & Assert
            assertThrows(RegraNegocioException.class, viagem::cancelar);
        }
    }

    @Nested
    class calcularValorPorMes {

        @Test
        @DisplayName("Deve dividir o faltante pelos meses restantes incluindo o atual, arredondando para cima")
        void deveDividirFaltantePelosMesesRestantes() {

            //Arrange
            var meta = comPrazo(LocalDate.of(2026, 12, 20));

            //Act & Assert
            assertEquals(new BigDecimal("333.34"), meta.calcularValorPorMes(BigDecimal.ZERO, HOJE));
            assertEquals(new BigDecimal("1000.00"), meta.calcularValorPorMes(BigDecimal.ZERO, LocalDate.of(2026, 12, 1)));
        }

        @Test
        @DisplayName("Deve retornar nulo sem prazo ou com prazo vencido")
        void deveRetornarNuloSemPrazoOuVencido() {

            //Arrange
            var meta = comPrazo(LocalDate.of(2026, 10, 31));
            var depoisDoPrazo = LocalDate.of(2026, 11, 1);

            //Act & Assert
            assertNull(viagem.calcularValorPorMes(BigDecimal.ZERO, HOJE));
            assertTrue(meta.isPrazoVencido(depoisDoPrazo));
            assertNull(meta.calcularValorPorMes(BigDecimal.ZERO, depoisDoPrazo));
        }
    }

    @Nested
    class calcularPercentual {

        @Test
        @DisplayName("Deve calcular o percentual acumulado do alvo")
        void deveCalcularPercentual() {

            //Act & Assert
            assertEquals(new BigDecimal("25.00"), viagem.calcularPercentual(new BigDecimal("250")));
        }
    }

    @Nested
    class getAportes {

        @Test
        @DisplayName("Deve impedir alteração da lista de aportes por fora da meta")
        void deveImpedirAlteracaoDaLista() {

            //Act & Assert
            assertThrows(UnsupportedOperationException.class, () -> viagem.getAportes().clear());
        }
    }
}
