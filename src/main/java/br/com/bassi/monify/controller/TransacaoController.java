package br.com.bassi.monify.controller;

import br.com.bassi.monify.config.UsuarioAutenticado;
import br.com.bassi.monify.dto.CriarTransacaoRequest;
import br.com.bassi.monify.dto.EditarTransacaoRequest;
import br.com.bassi.monify.dto.FiltroTransacoesRequest;
import br.com.bassi.monify.dto.PaginaResponse;
import br.com.bassi.monify.dto.TransacaoResponse;
import br.com.bassi.monify.service.TransacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/transacoes")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;

    @GetMapping
    public ResponseEntity<PaginaResponse<TransacaoResponse>> listar(@UsuarioAutenticado UUID usuarioId,
                                                                    @ParameterObject @Valid FiltroTransacoesRequest filtro,
                                                                    @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(transacaoService.listar(usuarioId, filtro, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoResponse> buscar(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        return ResponseEntity.ok(transacaoService.buscar(usuarioId, id));
    }

    @PostMapping
    public ResponseEntity<TransacaoResponse> criar(@UsuarioAutenticado UUID usuarioId,
                                                   @RequestBody @Valid CriarTransacaoRequest request) {

        var transacao = transacaoService.criar(usuarioId, request);
        return ResponseEntity.created(URI.create("/api/transacoes/" + transacao.id())).body(transacao);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editar(@UsuarioAutenticado UUID usuarioId,
                                       @PathVariable UUID id,
                                       @RequestBody @Valid EditarTransacaoRequest request) {

        transacaoService.editar(usuarioId, id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        transacaoService.excluir(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
