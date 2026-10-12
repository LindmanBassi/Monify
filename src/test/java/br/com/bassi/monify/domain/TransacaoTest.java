package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransacaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 9);

    private final UUID usuarioId = UUID.randomUUID();
    private final Categoria mercado = new Categoria(usuarioId, "Mercado", TipoTransacao.DESPESA, "#FF0000");

    @Nested
    class criar {

        @Test
        @DisplayName("Deve criar transação quando os dados forem válidos")
        void deveCriarTransacaoQuandoDadosValidos() {

            //Act
            var transacao = new Transacao(usuarioId, mercado, TipoTransacao.DESPESA,
                    new BigDecimal("52.9"), "Feira", HOJE);

            //Assert
            assertEquals(new BigDecimal("52.90"), transacao.getValor());
            assertSame(mercado, transacao.getCategoria());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o tipo for diferente do tipo da categoria")
        void deveLancarExcecaoQuandoTipoDiferenteDaCategoria() {

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> new Transacao(usuarioId, mercado,
                    TipoTransacao.RECEITA, BigDecimal.TEN, null, HOJE));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a categoria estiver arquivada")
        void deveLancarExcecaoQuandoCategoriaArquivada() {

            //Arrange
            mercado.arquivar();

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> new Transacao(usuarioId, mercado,
                    TipoTransacao.DESPESA, BigDecimal.TEN, null, HOJE));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a categoria for de outro usuário")
        void deveLancarExcecaoQuandoCategoriaDeOutroUsuario() {

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> new Transacao(UUID.randomUUID(), mercado,
                    TipoTransacao.DESPESA, BigDecimal.TEN, null, HOJE));
        }

        @Test
        @DisplayName("Deve lançar exceção quando o valor não for positivo ou tiver mais de duas casas")
        void deveLancarExcecaoQuandoValorInvalido() {

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> new Transacao(usuarioId, mercado,
                    TipoTransacao.DESPESA, BigDecimal.ZERO, null, HOJE));
            assertThrows(RegraNegocioException.class, () -> new Transacao(usuarioId, mercado,
                    TipoTransacao.DESPESA, new BigDecimal("1.999"), null, HOJE));
        }
    }

    @Nested
    class atualizar {

        @Test
        @DisplayName("Deve aplicar as mesmas validações da criação")
        void deveAplicarMesmasValidacoesDaCriacao() {

            //Arrange
            var transacao = new Transacao(usuarioId, mercado, TipoTransacao.DESPESA, BigDecimal.TEN, null, HOJE);
            var salario = new Categoria(usuarioId, "Salário", TipoTransacao.RECEITA, "#00FF00");

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> transacao.atualizar(salario,
                    TipoTransacao.DESPESA, BigDecimal.TEN, null, HOJE));
        }

        @Test
        @DisplayName("Deve atualizar os dados quando forem válidos")
        void deveAtualizarQuandoDadosValidos() {

            //Arrange
            var transacao = new Transacao(usuarioId, mercado, TipoTransacao.DESPESA, BigDecimal.TEN, null, HOJE);
            var salario = new Categoria(usuarioId, "Salário", TipoTransacao.RECEITA, "#00FF00");

            //Act
            transacao.atualizar(salario, TipoTransacao.RECEITA, new BigDecimal("5000"), "Outubro", HOJE);

            //Assert
            assertEquals(TipoTransacao.RECEITA, transacao.getTipo());
            assertEquals(new BigDecimal("5000.00"), transacao.getValor());
        }
    }
}
