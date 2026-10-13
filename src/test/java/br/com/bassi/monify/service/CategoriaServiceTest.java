package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.dto.CriarCategoriaRequest;
import br.com.bassi.monify.dto.EditarCategoriaRequest;
import br.com.bassi.monify.exception.ConflitoException;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.repository.CategoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID CATEGORIA_ID = UUID.randomUUID();

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Captor
    private ArgumentCaptor<Categoria> categoriaCaptor;

    private Categoria mercado() {
        return new Categoria(USUARIO_ID, "Mercado", TipoTransacao.DESPESA, "#FF0000");
    }

    @Nested
    class buscar {

        @Test
        @DisplayName("Deve lançar não encontrado quando a categoria não existir ou for de outro usuário")
        void deveLancarNaoEncontradoQuandoCategoriaDeOutroUsuario() {

            //Arrange
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.empty());

            //Act & Assert
            var excecao = assertThrows(RecursoNaoEncontradoException.class,
                    () -> categoriaService.buscar(USUARIO_ID, CATEGORIA_ID));
            assertEquals("Categoria não encontrada", excecao.getMessage());
        }
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve criar a categoria para o usuário com o nome sem espaços extras")
        void deveCriarCategoriaParaUsuario() {

            //Arrange
            var request = new CriarCategoriaRequest(" Pets ", TipoTransacao.DESPESA, "#123456");
            when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            //Act
            var response = categoriaService.criar(USUARIO_ID, request);

            //Assert
            verify(categoriaRepository).save(categoriaCaptor.capture());
            var salva = categoriaCaptor.getValue();
            assertTrue(salva.pertenceA(USUARIO_ID));
            assertEquals("Pets", salva.getNome());
            assertEquals(TipoTransacao.DESPESA, salva.getTipo());
            assertEquals("#123456", response.cor());
        }

        @Test
        @DisplayName("Deve lançar conflito quando já existir categoria com o mesmo nome e tipo")
        void deveLancarConflitoQuandoNomeDuplicado() {

            //Arrange
            var request = new CriarCategoriaRequest("Mercado", TipoTransacao.DESPESA, "#123456");
            when(categoriaRepository.existsByUsuarioIdAndNomeAndTipo(USUARIO_ID, "Mercado", TipoTransacao.DESPESA))
                    .thenReturn(true);

            //Act & Assert
            var excecao = assertThrows(ConflitoException.class, () -> categoriaService.criar(USUARIO_ID, request));
            assertEquals("Já existe uma categoria de despesa com esse nome", excecao.getMessage());
            verify(categoriaRepository, never()).save(any());
        }
    }

    @Nested
    class editar {

        @Test
        @DisplayName("Deve editar nome e cor da categoria do usuário")
        void deveEditarNomeECor() {

            //Arrange
            var categoria = mercado();
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(categoria));

            //Act
            categoriaService.editar(USUARIO_ID, CATEGORIA_ID, new EditarCategoriaRequest("Supermercado", "#00FF00"));

            //Assert
            assertEquals("Supermercado", categoria.getNome());
            assertEquals("#00FF00", categoria.getCor());
        }

        @Test
        @DisplayName("Deve lançar conflito quando outra categoria do mesmo tipo já usar o nome")
        void deveLancarConflitoQuandoOutraCategoriaUsaNome() {

            //Arrange
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado()));
            when(categoriaRepository.existsByUsuarioIdAndNomeAndTipoAndIdNot(USUARIO_ID, "Lazer", TipoTransacao.DESPESA, CATEGORIA_ID))
                    .thenReturn(true);

            //Act & Assert
            assertThrows(ConflitoException.class,
                    () -> categoriaService.editar(USUARIO_ID, CATEGORIA_ID, new EditarCategoriaRequest("Lazer", "#00FF00")));
        }
    }

    @Nested
    class arquivar {

        @Test
        @DisplayName("Deve arquivar a categoria do usuário")
        void deveArquivarCategoria() {

            //Arrange
            var categoria = mercado();
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(categoria));

            //Act
            categoriaService.arquivar(USUARIO_ID, CATEGORIA_ID);

            //Assert
            assertTrue(categoria.isArquivada());
        }
    }

    @Nested
    class desarquivar {

        @Test
        @DisplayName("Deve desarquivar a categoria do usuário")
        void deveDesarquivarCategoria() {

            //Arrange
            var categoria = mercado();
            categoria.arquivar();
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(categoria));

            //Act
            categoriaService.desarquivar(USUARIO_ID, CATEGORIA_ID);

            //Assert
            assertFalse(categoria.isArquivada());
        }
    }
}
