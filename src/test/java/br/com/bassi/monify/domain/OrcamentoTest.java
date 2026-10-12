package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.StatusOrcamento;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrcamentoTest {

    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);

    private final UUID usuarioId = UUID.randomUUID();
    private final Categoria mercado = new Categoria(usuarioId, "Mercado", TipoTransacao.DESPESA, "#FF0000");

    private Orcamento orcamentoDeMil() {
        return new Orcamento(usuarioId, mercado, OUTUBRO, new BigDecimal("1000"));
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve lançar exceção quando a categoria for de receita")
        void deveLancarExcecaoQuandoCategoriaDeReceita() {

            //Arrange
            var salario = new Categoria(usuarioId, "Salário", TipoTransacao.RECEITA, "#00FF00");

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> new Orcamento(usuarioId, salario, OUTUBRO, BigDecimal.TEN));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a categoria for de outro usuário")
        void deveLancarExcecaoQuandoCategoriaDeOutroUsuario() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> new Orcamento(UUID.randomUUID(), mercado, OUTUBRO, BigDecimal.TEN));
        }

        @Test
        @DisplayName("Deve lançar exceção quando o limite não for positivo")
        void deveLancarExcecaoQuandoLimiteNaoPositivo() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> new Orcamento(usuarioId, mercado, OUTUBRO, BigDecimal.ZERO));
        }
    }

    @Nested
    class calcularStatus {

        @ParameterizedTest(name = "gasto {0} -> {1}")
        @DisplayName("Deve calcular o status pelos limites de 80% e 100%")
        @CsvSource({
                "0,       DENTRO_DO_LIMITE",
                "800.00,  DENTRO_DO_LIMITE",
                "800.01,  PROXIMO_DO_LIMITE",
                "1000,    PROXIMO_DO_LIMITE",
                "1000.01, ESTOURADO"
        })
        void deveCalcularStatusPelosLimites(BigDecimal gasto, StatusOrcamento esperado) {

            //Arrange
            var orcamento = orcamentoDeMil();

            //Act
            var status = orcamento.calcularStatus(gasto);

            //Assert
            assertEquals(esperado, status);
        }
    }

    @Nested
    class calcularRestante {

        @Test
        @DisplayName("Deve retornar restante negativo quando o orçamento estourar")
        void deveRetornarRestanteNegativoQuandoEstourado() {

            //Act
            var restante = orcamentoDeMil().calcularRestante(new BigDecimal("1200.00"));

            //Assert
            assertEquals(new BigDecimal("-200.00"), restante);
        }
    }

    @Nested
    class alterarLimite {

        @Test
        @DisplayName("Deve alterar o limite do orçamento")
        void deveAlterarLimite() {

            //Arrange
            var orcamento = orcamentoDeMil();

            //Act
            orcamento.alterarLimite(new BigDecimal("1500"));

            //Assert
            assertEquals(new BigDecimal("1500.00"), orcamento.getValorLimite());
        }
    }
}
