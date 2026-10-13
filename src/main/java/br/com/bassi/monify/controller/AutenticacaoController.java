package br.com.bassi.monify.controller;

import br.com.bassi.monify.dto.CadastroRequest;
import br.com.bassi.monify.dto.LoginRequest;
import br.com.bassi.monify.dto.TokenRenovacaoRequest;
import br.com.bassi.monify.dto.TokensResponse;
import br.com.bassi.monify.dto.UsuarioResponse;
import br.com.bassi.monify.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid CadastroRequest request) {

        var usuario = autenticacaoService.cadastrar(request);
        return ResponseEntity.created(URI.create("/api/usuarios/me")).body(usuario);
    }

    @PostMapping("/login")
    public ResponseEntity<TokensResponse> login(@RequestBody @Valid LoginRequest request) {

        return ResponseEntity.ok(autenticacaoService.login(request));
    }

    @PostMapping("/renovar")
    public ResponseEntity<TokensResponse> renovar(@RequestBody @Valid TokenRenovacaoRequest request) {

        return ResponseEntity.ok(autenticacaoService.renovar(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid TokenRenovacaoRequest request) {

        autenticacaoService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
