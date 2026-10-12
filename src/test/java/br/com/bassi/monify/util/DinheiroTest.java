package br.com.bassi.monify.util;

import br.com.bassi.monify.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DinheiroTest {

    @Nested
    class normalizar {

        @Test
        @DisplayName("Deve normalizar o valor para duas casas decimais")
        void deveNormalizarParaDuasCasasDecimais() {

            //Act
            var inteiro = Dinheiro.normalizar(new BigDecimal("10"), "Valor");
            var umaCasa = Dinheiro.normalizar(new BigDecimal("10.5"), "Valor");

            //Assert
            assertEquals(new BigDecimal("10.00"), inteiro);
            assertEquals(new BigDecimal("10.50"), umaCasa);
        }

        @Test
        @DisplayName("Deve aceitar zeros excedentes além dos centavos")
        void deveAceitarZerosExcedentes() {

            //Act
            var valor = Dinheiro.normalizar(new BigDecimal("10.500"), "Valor");

            //Assert
            assertEquals(new BigDecimal("10.50"), valor);
        }

        @Test
        @DisplayName("Deve lançar exceção quando o valor tiver mais de duas casas decimais")
        void deveLancarExcecaoQuandoMaisDeDuasCasas() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> Dinheiro.normalizar(new BigDecimal("10.001"), "Valor"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando o valor for nulo")
        void deveLancarExcecaoQuandoNulo() {

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> Dinheiro.normalizar(null, "Valor"));
        }
    }

    @Nested
    class validarPositivo {

        @Test
        @DisplayName("Deve lançar exceção quando o valor for zero ou negativo")
        void deveLancarExcecaoQuandoZeroOuNegativo() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> Dinheiro.validarPositivo(BigDecimal.ZERO, "Valor"));
            assertThrows(RegraNegocioException.class,
                    () -> Dinheiro.validarPositivo(new BigDecimal("-1"), "Valor"));
        }

        @Test
        @DisplayName("Deve retornar o valor normalizado quando for positivo")
        void deveRetornarValorNormalizadoQuandoPositivo() {

            //Act
            var valor = Dinheiro.validarPositivo(new BigDecimal("0.01"), "Valor");

            //Assert
            assertEquals(new BigDecimal("0.01"), valor);
        }
    }
}
