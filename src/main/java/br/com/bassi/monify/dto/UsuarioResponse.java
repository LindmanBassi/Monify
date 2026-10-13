package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.Usuario;

import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String email) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
