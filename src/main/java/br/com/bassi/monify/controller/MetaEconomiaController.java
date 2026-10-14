package br.com.bassi.monify.controller;

import br.com.bassi.monify.config.UsuarioAutenticado;
import br.com.bassi.monify.dto.CriarMetaEconomiaRequest;
import br.com.bassi.monify.dto.EditarMetaEconomiaRequest;
import br.com.bassi.monify.dto.MetaEconomiaResponse;
import br.com.bassi.monify.service.MetaEconomiaService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/metas-economia")
@RequiredArgsConstructor
public class MetaEconomiaController {

    private final MetaEconomiaService metaEconomiaService;

    @GetMapping
    public ResponseEntity<List<MetaEconomiaResponse>> listar(
            @UsuarioAutenticado UUID usuarioId,
            @Parameter(description = "Mês no formato yyyy-MM (padrão: mês atual)", schema = @Schema(type = "string", example = "2026-10"))
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {

        return ResponseEntity.ok(metaEconomiaService.listar(usuarioId, mes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MetaEconomiaResponse> buscar(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        return ResponseEntity.ok(metaEconomiaService.buscar(usuarioId, id));
    }

    @PostMapping
    public ResponseEntity<MetaEconomiaResponse> criar(@UsuarioAutenticado UUID usuarioId,
                                                      @RequestBody @Valid CriarMetaEconomiaRequest request) {

        var meta = metaEconomiaService.criar(usuarioId, request);
        return ResponseEntity.created(URI.create("/api/metas-economia/" + meta.id())).body(meta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editar(@UsuarioAutenticado UUID usuarioId,
                                       @PathVariable UUID id,
                                       @RequestBody @Valid EditarMetaEconomiaRequest request) {

        metaEconomiaService.editar(usuarioId, id, request);
        return ResponseEntity.noContent().build();
    }

    // metas não são excluídas: canceladas permanecem no histórico
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        metaEconomiaService.cancelar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
