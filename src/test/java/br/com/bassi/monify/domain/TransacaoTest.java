package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransacaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 9);

    private final UUID usuarioId = UUID.randomUUID();
    private final Categoria mercado = categoriaComId("Mercado", TipoTransacao.DESPESA);
    private final Categoria salario = categoriaComId("Salário", TipoTransacao.RECEITA);

    private Categoria categoriaComId(String nome, TipoTransacao tipo) {
        var categoria = new Categoria(usuarioId, nome, tipo, "#FF0000");
        ReflectionTestUtils.setField(categoria, "id", UUID.randomUUID());
        return categoria;
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve criar transação com o tipo da categoria quando os dados forem válidos")
        void deveCriarTransacaoComTipoDaCategoria() {

            //Act
            var transacao = new Transacao(usuarioId, mercado, new BigDecimal("52.9"), " Feira ", HOJE, HOJE);

            //Assert
            assertEquals(new BigDecimal("52.90"), transacao.getValor());
            assertEquals(TipoTransacao.DESPESA, transacao.getTipo());
            assertEquals("Feira", transacao.getDescricao());
            assertSame(mercado, transacao.getCategoria());
        }

        @Test
        @DisplayName("Deve guardar descrição em branco como nula")
        void deveGuardarDescricaoEmBrancoComoNula() {

            //Act
            var transacao = new Transacao(usuarioId, mercado, BigDecimal.TEN, "   ", HOJE, HOJE);

            //Assert
            assertNull(transacao.getDescricao());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a categoria estiver arquivada")
        void deveLancarExcecaoQuandoCategoriaArquivada() {

            //Arrange
            mercado.arquivar();

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> new Transacao(usuarioId, mercado, BigDecimal.TEN, null, HOJE, HOJE));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a categoria for de outro usuário")
        void deveLancarExcecaoQuandoCategoriaDeOutroUsuario() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> new Transacao(UUID.randomUUID(), mercado, BigDecimal.TEN, null, HOJE, HOJE));
        }

        @Test
        @DisplayName("Deve lançar exceção quando o valor não for positivo ou tiver mais de duas casas")
        void deveLancarExcecaoQuandoValorInvalido() {

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> new Transacao(usuarioId, mercado, BigDecimal.ZERO, null, HOJE, HOJE));
            assertThrows(RegraNegocioException.class,
                    () -> new Transacao(usuarioId, mercado, new BigDecimal("1.999"), null, HOJE, HOJE));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a data for futura")
        void deveLancarExcecaoQuandoDataFutura() {

            //Act & Assert
            var excecao = assertThrows(RegraNegocioException.class,
                    () -> new Transacao(usuarioId, mercado, BigDecimal.TEN, null, HOJE.plusDays(1), HOJE));
            assertEquals("Data da transação não pode ser futura", excecao.getMessage());
        }
    }

    @Nested
    class atualizar {

        @Test
        @DisplayName("Deve atualizar os dados e o tipo quando a categoria mudar")
        void deveAtualizarDadosETipoQuandoCategoriaMudar() {

            //Arrange
            var transacao = new Transacao(usuarioId, mercado, BigDecimal.TEN, null, HOJE, HOJE);

            //Act
            transacao.atualizar(salario, new BigDecimal("5000"), "Outubro", HOJE.minusDays(1), HOJE);

            //Assert
            assertEquals(TipoTransacao.RECEITA, transacao.getTipo());
            assertEquals(new BigDecimal("5000.00"), transacao.getValor());
            assertEquals("Outubro", transacao.getDescricao());
            assertEquals(HOJE.minusDays(1), transacao.getDataOcorrencia());
        }

        @Test
        @DisplayName("Deve permitir editar transação de categoria arquivada quando a categoria for mantida")
        void devePermitirEditarQuandoCategoriaArquivadaMantida() {

            //Arrange
            var transacao = new Transacao(usuarioId, mercado, BigDecimal.TEN, null, HOJE, HOJE);
            mercado.arquivar();

            //Act
            transacao.atualizar(mercado, new BigDecimal("12"), "Corrigido", HOJE, HOJE);

            //Assert
            assertEquals(new BigDecimal("12.00"), transacao.getValor());
            assertEquals("Corrigido", transacao.getDescricao());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a transação for movida para categoria arquivada")
        void deveLancarExcecaoQuandoMovidaParaCategoriaArquivada() {

            //Arrange
            var transacao = new Transacao(usuarioId, mercado, BigDecimal.TEN, null, HOJE, HOJE);
            salario.arquivar();

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> transacao.atualizar(salario, BigDecimal.TEN, null, HOJE, HOJE));
        }

        @Test
        @DisplayName("Deve aplicar as validações de valor e data da criação")
        void deveAplicarValidacoesDaCriacao() {

            //Arrange
            var transacao = new Transacao(usuarioId, mercado, BigDecimal.TEN, null, HOJE, HOJE);

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> transacao.atualizar(mercado, BigDecimal.ZERO, null, HOJE, HOJE));
            assertThrows(RegraNegocioException.class,
                    () -> transacao.atualizar(mercado, BigDecimal.TEN, null, HOJE.plusDays(1), HOJE));
        }
    }
}
