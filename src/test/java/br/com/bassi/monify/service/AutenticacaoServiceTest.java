package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.TokenRenovacao;
import br.com.bassi.monify.domain.Usuario;
import br.com.bassi.monify.domain.enums.CategoriaPadrao;
import br.com.bassi.monify.dto.CadastroRequest;
import br.com.bassi.monify.dto.LoginRequest;
import br.com.bassi.monify.exception.ConflitoException;
import br.com.bassi.monify.exception.NaoAutenticadoException;
import br.com.bassi.monify.repository.CategoriaRepository;
import br.com.bassi.monify.repository.TokenRenovacaoRepository;
import br.com.bassi.monify.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-12T15:00:00Z");
    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private TokenRenovacaoRepository tokenRenovacaoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;

    @Captor
    private ArgumentCaptor<List<Categoria>> categoriasCaptor;

    @Captor
    private ArgumentCaptor<TokenRenovacao> tokenCaptor;

    private AutenticacaoService autenticacaoService;

    @BeforeEach
    void setUp() {
        autenticacaoService = new AutenticacaoService(
                usuarioRepository,
                categoriaRepository,
                tokenRenovacaoRepository,
                passwordEncoder,
                tokenService,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private Usuario usuarioComId() {
        var usuario = new Usuario("Henrique", "henrique@email.com", "senha-hash");
        ReflectionTestUtils.setField(usuario, "id", USUARIO_ID);
        return usuario;
    }

    private void prepararEmissaoDeTokens() {
        when(tokenService.gerarTokenAcesso(USUARIO_ID)).thenReturn("token-acesso");
        when(tokenService.gerarTokenRenovacao()).thenReturn("novo-token-renovacao");
        when(tokenService.calcularHash(anyString())).thenAnswer(inv -> "hash-" + inv.getArgument(0));
        when(tokenService.calcularExpiracaoTokenRenovacao()).thenReturn(AGORA.plusSeconds(3600));
        when(tokenService.duracaoTokenAcessoEmSegundos()).thenReturn(900L);
    }

    @Nested
    class cadastrar {

        private final CadastroRequest request = new CadastroRequest(" Henrique ", " Henrique@Email.com ", "senha-forte");

        @Test
        @DisplayName("Deve lançar conflito quando o e-mail já estiver cadastrado")
        void deveLancarConflitoQuandoEmailJaCadastrado() {

            //Arrange
            when(usuarioRepository.existsByEmail("henrique@email.com")).thenReturn(true);

            //Act & Assert
            var excecao = assertThrows(ConflitoException.class, () -> autenticacaoService.cadastrar(request));
            assertEquals("E-mail já cadastrado", excecao.getMessage());
            verify(usuarioRepository, never()).save(any());
            verifyNoInteractions(categoriaRepository);
        }

        @Test
        @DisplayName("Deve salvar o usuário com a senha criptografada e o e-mail normalizado")
        void deveSalvarUsuarioComSenhaCriptografada() {

            //Arrange
            when(passwordEncoder.encode("senha-forte")).thenReturn("senha-hash");
            when(usuarioRepository.save(any())).thenAnswer(inv -> {
                Usuario usuario = inv.getArgument(0);
                ReflectionTestUtils.setField(usuario, "id", USUARIO_ID);
                return usuario;
            });

            //Act
            var response = autenticacaoService.cadastrar(request);

            //Assert
            verify(usuarioRepository).save(usuarioCaptor.capture());
            var salvo = usuarioCaptor.getValue();
            assertEquals("Henrique", salvo.getNome());
            assertEquals("henrique@email.com", salvo.getEmail());
            assertEquals("senha-hash", salvo.getSenhaHash());
            assertEquals(USUARIO_ID, response.id());
        }

        @Test
        @DisplayName("Deve copiar as categorias padrão para o usuário cadastrado")
        void deveCopiarCategoriasPadrao() {

            //Arrange
            when(passwordEncoder.encode("senha-forte")).thenReturn("senha-hash");
            when(usuarioRepository.save(any())).thenAnswer(inv -> {
                Usuario usuario = inv.getArgument(0);
                ReflectionTestUtils.setField(usuario, "id", USUARIO_ID);
                return usuario;
            });

            //Act
            autenticacaoService.cadastrar(request);

            //Assert
            verify(categoriaRepository).saveAll(categoriasCaptor.capture());
            var categorias = categoriasCaptor.getValue();
            assertEquals(CategoriaPadrao.values().length, categorias.size());
            assertTrue(categorias.stream().allMatch(c -> c.pertenceA(USUARIO_ID)));
        }
    }

    @Nested
    class login {

        @Test
        @DisplayName("Deve lançar credenciais inválidas quando o e-mail não existir")
        void deveLancarCredenciaisInvalidasQuandoEmailNaoExistir() {

            //Arrange
            when(usuarioRepository.findByEmail("henrique@email.com")).thenReturn(Optional.empty());

            //Act & Assert
            var excecao = assertThrows(NaoAutenticadoException.class,
                    () -> autenticacaoService.login(new LoginRequest("Henrique@Email.com", "senha-forte")));
            assertEquals("Credenciais inválidas", excecao.getMessage());
        }

        @Test
        @DisplayName("Deve lançar a mesma mensagem de credenciais inválidas quando a senha estiver errada")
        void deveLancarCredenciaisInvalidasQuandoSenhaErrada() {

            //Arrange
            when(usuarioRepository.findByEmail("henrique@email.com")).thenReturn(Optional.of(usuarioComId()));
            when(passwordEncoder.matches("senha-errada", "senha-hash")).thenReturn(false);

            //Act & Assert
            var excecao = assertThrows(NaoAutenticadoException.class,
                    () -> autenticacaoService.login(new LoginRequest("henrique@email.com", "senha-errada")));
            assertEquals("Credenciais inválidas", excecao.getMessage());
            verifyNoInteractions(tokenRenovacaoRepository);
        }

        @Test
        @DisplayName("Deve emitir os tokens e salvar apenas o hash do token de renovação")
        void deveEmitirTokensQuandoCredenciaisValidas() {

            //Arrange
            when(usuarioRepository.findByEmail("henrique@email.com")).thenReturn(Optional.of(usuarioComId()));
            when(passwordEncoder.matches("senha-forte", "senha-hash")).thenReturn(true);
            prepararEmissaoDeTokens();

            //Act
            var response = autenticacaoService.login(new LoginRequest("henrique@email.com", "senha-forte"));

            //Assert
            assertEquals("token-acesso", response.tokenAcesso());
            assertEquals("novo-token-renovacao", response.tokenRenovacao());
            assertEquals(900L, response.expiraEmSegundos());

            verify(tokenRenovacaoRepository).save(tokenCaptor.capture());
            var salvo = tokenCaptor.getValue();
            assertEquals(USUARIO_ID, salvo.getUsuarioId());
            assertEquals("hash-novo-token-renovacao", salvo.getTokenHash());
            assertEquals(AGORA.plusSeconds(3600), salvo.getExpiraEm());
        }
    }

    @Nested
    class renovar {

        private final String request = "token-renovacao";

        @Test
        @DisplayName("Deve lançar não autenticado quando o token de renovação não existir")
        void deveLancarNaoAutenticadoQuandoTokenNaoExistir() {

            //Arrange
            when(tokenService.calcularHash("token-renovacao")).thenReturn("hash-token-renovacao");
            when(tokenRenovacaoRepository.findByTokenHash("hash-token-renovacao")).thenReturn(Optional.empty());

            //Act & Assert
            var excecao = assertThrows(NaoAutenticadoException.class, () -> autenticacaoService.renovar(request));
            assertEquals("Token de renovação inválido", excecao.getMessage());
        }

        @Test
        @DisplayName("Deve revogar todos os tokens do usuário quando um token já revogado for reutilizado")
        void deveRevogarTodosOsTokensQuandoTokenRevogadoForReutilizado() {

            //Arrange
            var token = new TokenRenovacao(USUARIO_ID, "hash-token-renovacao", AGORA.plusSeconds(3600));
            token.revogar(AGORA.minusSeconds(60));
            when(tokenService.calcularHash("token-renovacao")).thenReturn("hash-token-renovacao");
            when(tokenRenovacaoRepository.findByTokenHash("hash-token-renovacao")).thenReturn(Optional.of(token));

            //Act & Assert
            assertThrows(NaoAutenticadoException.class, () -> autenticacaoService.renovar(request));
            verify(tokenRenovacaoRepository).revogarTodosDoUsuario(USUARIO_ID, AGORA);
            verify(tokenRenovacaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar não autenticado quando o token de renovação estiver expirado")
        void deveLancarNaoAutenticadoQuandoTokenExpirado() {

            //Arrange
            var token = new TokenRenovacao(USUARIO_ID, "hash-token-renovacao", AGORA);
            when(tokenService.calcularHash("token-renovacao")).thenReturn("hash-token-renovacao");
            when(tokenRenovacaoRepository.findByTokenHash("hash-token-renovacao")).thenReturn(Optional.of(token));

            //Act & Assert
            var excecao = assertThrows(NaoAutenticadoException.class, () -> autenticacaoService.renovar(request));
            assertEquals("Token de renovação expirado", excecao.getMessage());
            verify(tokenRenovacaoRepository, never()).revogarTodosDoUsuario(any(), any());
        }

        @Test
        @DisplayName("Deve revogar o token usado e emitir um novo par quando o token for válido")
        void deveRotacionarQuandoTokenValido() {

            //Arrange
            var token = new TokenRenovacao(USUARIO_ID, "hash-token-renovacao", AGORA.plusSeconds(3600));
            prepararEmissaoDeTokens();
            when(tokenRenovacaoRepository.findByTokenHash("hash-token-renovacao")).thenReturn(Optional.of(token));

            //Act
            var response = autenticacaoService.renovar(request);

            //Assert
            assertEquals(AGORA, token.getRevogadoEm());
            assertEquals("novo-token-renovacao", response.tokenRenovacao());
            verify(tokenRenovacaoRepository).save(tokenCaptor.capture());
            assertEquals("hash-novo-token-renovacao", tokenCaptor.getValue().getTokenHash());
        }
    }

    @Nested
    class logout {

        @Test
        @DisplayName("Deve revogar o token de renovação informado")
        void deveRevogarTokenInformado() {

            //Arrange
            var token = new TokenRenovacao(USUARIO_ID, "hash-token-renovacao", AGORA.plusSeconds(3600));
            when(tokenService.calcularHash("token-renovacao")).thenReturn("hash-token-renovacao");
            when(tokenRenovacaoRepository.findByTokenHash("hash-token-renovacao")).thenReturn(Optional.of(token));

            //Act
            autenticacaoService.logout("token-renovacao");

            //Assert
            assertEquals(AGORA, token.getRevogadoEm());
        }

        @Test
        @DisplayName("Deve ignorar token de renovação inexistente")
        void deveIgnorarTokenInexistente() {

            //Arrange
            when(tokenService.calcularHash("token-renovacao")).thenReturn("hash-token-renovacao");
            when(tokenRenovacaoRepository.findByTokenHash("hash-token-renovacao")).thenReturn(Optional.empty());

            //Act & Assert
            assertDoesNotThrow(() -> autenticacaoService.logout("token-renovacao"));
        }
    }
}
