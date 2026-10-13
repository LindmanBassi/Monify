package br.com.bassi.monify.controller;

import br.com.bassi.monify.dto.UsuarioResponse;
import br.com.bassi.monify.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // o ID do usuário vem sempre do token, nunca do request
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> buscarAutenticado(@AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(usuarioService.buscar(UUID.fromString(jwt.getSubject())));
    }
}
