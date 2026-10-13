package br.com.bassi.monify.service;

import br.com.bassi.monify.dto.UsuarioResponse;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(UUID usuarioId) {

        return usuarioRepository.findById(usuarioId)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
