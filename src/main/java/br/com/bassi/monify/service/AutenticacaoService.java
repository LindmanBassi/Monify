package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.TokenRenovacao;
import br.com.bassi.monify.domain.Usuario;
import br.com.bassi.monify.domain.enums.CategoriaPadrao;
import br.com.bassi.monify.dto.CadastroRequest;
import br.com.bassi.monify.dto.LoginRequest;
import br.com.bassi.monify.dto.UsuarioResponse;
import br.com.bassi.monify.exception.ConflitoException;
import br.com.bassi.monify.exception.NaoAutenticadoException;
import br.com.bassi.monify.repository.CategoriaRepository;
import br.com.bassi.monify.repository.TokenRenovacaoRepository;
import br.com.bassi.monify.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final TokenRenovacaoRepository tokenRenovacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest request) {

        // 1. validar e-mail único
        var email = Usuario.normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("E-mail já cadastrado");
        }

        // 2. salvar o usuário com a senha criptografada
        var usuario = usuarioRepository.save(new Usuario(request.nome(), email, passwordEncoder.encode(request.senha())));

        // 3. copiar as categorias padrão para o usuário
        categoriaRepository.saveAll(CategoriaPadrao.criarPara(usuario.getId()));

        return UsuarioResponse.from(usuario);
    }

    // mensagem genérica: não revela se o e-mail existe
    @Transactional
    public SessaoEmitida login(LoginRequest request) {

        var usuario = usuarioRepository.findByEmail(Usuario.normalizarEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new NaoAutenticadoException("Credenciais inválidas"));

        return emitirTokens(usuario.getId());
    }

    // rotação: o token usado é revogado e um novo par é emitido
    @Transactional(noRollbackFor = NaoAutenticadoException.class)
    public SessaoEmitida renovar(String tokenRenovacao) {

        var agora = Instant.now(clock);
        var token = buscarTokenRenovacao(tokenRenovacao);

        // 1. reuso de token revogado: revoga todos os tokens do usuário e exige novo login
        if (token.isRevogado()) {
            tokenRenovacaoRepository.revogarTodosDoUsuario(token.getUsuarioId(), agora);
            throw new NaoAutenticadoException("Sessão encerrada por segurança. Faça login novamente");
        }

        // 2. token expirado exige novo login
        if (token.isExpirado(agora)) {
            throw new NaoAutenticadoException("Token de renovação expirado");
        }

        // 3. revogar o token usado e emitir um novo par
        token.revogar(agora);
        return emitirTokens(token.getUsuarioId());
    }

    @Transactional
    public void logout(String tokenRenovacao) {

        tokenRenovacaoRepository.findByTokenHash(tokenService.calcularHash(tokenRenovacao))
                .ifPresent(token -> token.revogar(Instant.now(clock)));
    }

    private TokenRenovacao buscarTokenRenovacao(String tokenRenovacao) {
        return tokenRenovacaoRepository.findByTokenHash(tokenService.calcularHash(tokenRenovacao))
                .orElseThrow(() -> new NaoAutenticadoException("Token de renovação inválido"));
    }

    private SessaoEmitida emitirTokens(UUID usuarioId) {

        var tokenAcesso = tokenService.gerarTokenAcesso(usuarioId);
        var tokenRenovacao = tokenService.gerarTokenRenovacao();

        tokenRenovacaoRepository.save(new TokenRenovacao(
                usuarioId,
                tokenService.calcularHash(tokenRenovacao),
                tokenService.calcularExpiracaoTokenRenovacao()));

        return new SessaoEmitida(tokenAcesso, tokenRenovacao, tokenService.duracaoTokenAcessoEmSegundos());
    }
}
