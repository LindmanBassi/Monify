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

    private final MetaPoupanca viagem = new MetaPoupanca(UUID.randomUUID(), "Viagem", new BigDecimal("1000"), null);

    @Nested
    class criar {

        @Test
        @DisplayName("Deve começar ativa e sem valor acumulado")
        void deveComecarAtivaESemAcumulado() {

            //Act & Assert
            assertEquals(StatusMetaPoupanca.ATIVA, viagem.getStatus());
            assertEquals(new BigDecimal("0.00"), viagem.calcularAcumulado());
            assertEquals(new BigDecimal("1000.00"), viagem.calcularFaltante());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o valor alvo não for positivo")
        void deveLancarExcecaoQuandoValorAlvoNaoPositivo() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> new MetaPoupanca(UUID.randomUUID(), "X", BigDecimal.ZERO, null));
        }
    }

    @Nested
    class registrarAporte {

        @Test
        @DisplayName("Deve acumular aportes e resgates")
        void deveAcumularAportesEResgates() {

            //Act
            var aporte = viagem.registrarAporte(new BigDecimal("300"), HOJE, "Salário");
            viagem.registrarAporte(new BigDecimal("-100"), HOJE, null);

            //Assert
            assertSame(viagem, aporte.getMetaPoupanca());
            assertEquals(2, viagem.getAportes().size());
            assertEquals(new BigDecimal("200.00"), viagem.calcularAcumulado());
            assertEquals(new BigDecimal("800.00"), viagem.calcularFaltante());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o resgate deixar o acumulado negativo")
        void deveLancarExcecaoQuandoAcumuladoFicarNegativo() {

            //Arrange
            viagem.registrarAporte(new BigDecimal("100"), HOJE, null);

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> viagem.registrarAporte(new BigDecimal("-100.01"), HOJE, null));
            assertEquals(1, viagem.getAportes().size());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o valor for zero")
        void deveLancarExcecaoQuandoValorZero() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> viagem.registrarAporte(BigDecimal.ZERO, HOJE, null));
        }

        @Test
        @DisplayName("Deve concluir a meta ao atingir o alvo")
        void deveConcluirAoAtingirAlvo() {

            //Act
            viagem.registrarAporte(new BigDecimal("1000"), HOJE, null);

            //Assert
            assertEquals(StatusMetaPoupanca.CONCLUIDA, viagem.getStatus());
            assertEquals(new BigDecimal("0.00"), viagem.calcularFaltante());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a meta não estiver ativa")
        void deveLancarExcecaoQuandoMetaNaoAtiva() {

            //Arrange
            viagem.registrarAporte(new BigDecimal("1000"), HOJE, null);

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> viagem.registrarAporte(new BigDecimal("10"), HOJE, null));
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
