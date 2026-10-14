package br.com.bassi.monify.controller;

import br.com.bassi.monify.config.CookieTokenRenovacao;
import br.com.bassi.monify.dto.CadastroRequest;
import br.com.bassi.monify.dto.LoginRequest;
import br.com.bassi.monify.dto.TokenAcessoResponse;
import br.com.bassi.monify.dto.UsuarioResponse;
import br.com.bassi.monify.exception.NaoAutenticadoException;
import br.com.bassi.monify.service.AutenticacaoService;
import br.com.bassi.monify.service.SessaoEmitida;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;
    private final CookieTokenRenovacao cookieTokenRenovacao;

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid CadastroRequest request) {

        var usuario = autenticacaoService.cadastrar(request);
        return ResponseEntity.created(URI.create("/api/usuarios/me")).body(usuario);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenAcessoResponse> login(@RequestBody @Valid LoginRequest request) {

        return comSessao(autenticacaoService.login(request));
    }

    @PostMapping("/renovar")
    public ResponseEntity<TokenAcessoResponse> renovar(
            @CookieValue(name = CookieTokenRenovacao.NOME, required = false) String tokenRenovacao) {

        if (tokenRenovacao == null || tokenRenovacao.isBlank()) {
            throw new NaoAutenticadoException("Sessão expirada. Faça login novamente");
        }
        return comSessao(autenticacaoService.renovar(tokenRenovacao));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = CookieTokenRenovacao.NOME, required = false) String tokenRenovacao) {

        if (tokenRenovacao != null && !tokenRenovacao.isBlank()) {
            autenticacaoService.logout(tokenRenovacao);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieTokenRenovacao.remover().toString())
                .build();
    }

    private ResponseEntity<TokenAcessoResponse> comSessao(SessaoEmitida sessao) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieTokenRenovacao.criar(sessao.tokenRenovacao()).toString())
                .body(TokenAcessoResponse.bearer(sessao.tokenAcesso(), sessao.expiraEmSegundos()));
    }
}
