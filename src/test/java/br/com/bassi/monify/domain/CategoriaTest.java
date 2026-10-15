package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.IconeCategoria;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoriaTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private final Categoria categoria = new Categoria(USUARIO_ID, " Mercado ", TipoTransacao.DESPESA, "#FF0000", IconeCategoria.ETIQUETA);

    @Nested
    class editar {

        @Test
        @DisplayName("Deve alterar nome, cor e ícone mantendo o tipo")
        void deveAlterarNomeCorEIconeMantendoTipo() {

            //Act
            categoria.editar(" Supermercado ", "#00FF00", IconeCategoria.CARRINHO);

            //Assert
            assertEquals("Supermercado", categoria.getNome());
            assertEquals("#00FF00", categoria.getCor());
            assertEquals(IconeCategoria.CARRINHO, categoria.getIcone());
            assertEquals(TipoTransacao.DESPESA, categoria.getTipo());
        }
    }

    @Nested
    class arquivar {

        @Test
        @DisplayName("Deve impedir o uso da categoria quando arquivada")
        void deveImpedirUsoQuandoArquivada() {

            //Act
            categoria.arquivar();

            //Assert
            assertTrue(categoria.isArquivada());
            var excecao = assertThrows(RegraNegocioException.class, () -> categoria.validarAtivaDoUsuario(USUARIO_ID));
            assertEquals("Categoria arquivada não pode ser utilizada", excecao.getMessage());
        }
    }

    @Nested
    class desarquivar {

        @Test
        @DisplayName("Deve liberar o uso da categoria quando desarquivada")
        void deveLiberarUsoQuandoDesarquivada() {

            //Arrange
            categoria.arquivar();

            //Act
            categoria.desarquivar();

            //Assert
            assertFalse(categoria.isArquivada());
            assertDoesNotThrow(() -> categoria.validarAtivaDoUsuario(USUARIO_ID));
        }
    }
}
