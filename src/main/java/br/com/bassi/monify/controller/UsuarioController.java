package br.com.bassi.monify.controller;

import br.com.bassi.monify.config.UsuarioAutenticado;
import br.com.bassi.monify.dto.UsuarioResponse;
import br.com.bassi.monify.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> buscarAutenticado(@UsuarioAutenticado UUID usuarioId) {

        return ResponseEntity.ok(usuarioService.buscar(usuarioId));
    }
}
