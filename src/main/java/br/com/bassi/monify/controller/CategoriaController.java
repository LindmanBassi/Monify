package br.com.bassi.monify.controller;

import br.com.bassi.monify.config.UsuarioAutenticado;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.dto.CategoriaResponse;
import br.com.bassi.monify.dto.CriarCategoriaRequest;
import br.com.bassi.monify.dto.EditarCategoriaRequest;
import br.com.bassi.monify.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar(@UsuarioAutenticado UUID usuarioId,
                                                          @RequestParam(required = false) TipoTransacao tipo,
                                                          @RequestParam(defaultValue = "false") boolean incluirArquivadas) {

        return ResponseEntity.ok(categoriaService.listar(usuarioId, tipo, incluirArquivadas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscar(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        return ResponseEntity.ok(categoriaService.buscar(usuarioId, id));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@UsuarioAutenticado UUID usuarioId,
                                                   @RequestBody @Valid CriarCategoriaRequest request) {

        var categoria = categoriaService.criar(usuarioId, request);
        return ResponseEntity.created(URI.create("/api/categorias/" + categoria.id())).body(categoria);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editar(@UsuarioAutenticado UUID usuarioId,
                                       @PathVariable UUID id,
                                       @RequestBody @Valid EditarCategoriaRequest request) {

        categoriaService.editar(usuarioId, id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> arquivar(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        categoriaService.arquivar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desarquivar")
    public ResponseEntity<Void> desarquivar(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        categoriaService.desarquivar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
