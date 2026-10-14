package br.com.bassi.monify.controller;

import br.com.bassi.monify.config.UsuarioAutenticado;
import br.com.bassi.monify.domain.enums.StatusMetaPoupanca;
import br.com.bassi.monify.dto.AporteResponse;
import br.com.bassi.monify.dto.CriarMetaPoupancaRequest;
import br.com.bassi.monify.dto.EditarMetaPoupancaRequest;
import br.com.bassi.monify.dto.MetaPoupancaResponse;
import br.com.bassi.monify.dto.PaginaResponse;
import br.com.bassi.monify.dto.RegistrarAporteRequest;
import br.com.bassi.monify.service.MetaPoupancaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/metas-poupanca")
@RequiredArgsConstructor
public class MetaPoupancaController {

    private final MetaPoupancaService metaPoupancaService;

    @GetMapping
    public ResponseEntity<List<MetaPoupancaResponse>> listar(@UsuarioAutenticado UUID usuarioId,
                                                             @RequestParam(required = false) StatusMetaPoupanca status) {

        return ResponseEntity.ok(metaPoupancaService.listar(usuarioId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MetaPoupancaResponse> buscar(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        return ResponseEntity.ok(metaPoupancaService.buscar(usuarioId, id));
    }

    @PostMapping
    public ResponseEntity<MetaPoupancaResponse> criar(@UsuarioAutenticado UUID usuarioId,
                                                      @RequestBody @Valid CriarMetaPoupancaRequest request) {

        var meta = metaPoupancaService.criar(usuarioId, request);
        return ResponseEntity.created(URI.create("/api/metas-poupanca/" + meta.id())).body(meta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editar(@UsuarioAutenticado UUID usuarioId,
                                       @PathVariable UUID id,
                                       @RequestBody @Valid EditarMetaPoupancaRequest request) {

        metaPoupancaService.editar(usuarioId, id, request);
        return ResponseEntity.noContent().build();
    }

    // metas não são excluídas: canceladas permanecem no histórico
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        metaPoupancaService.cancelar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aportes")
    public ResponseEntity<AporteResponse> registrarAporte(@UsuarioAutenticado UUID usuarioId,
                                                          @PathVariable UUID id,
                                                          @RequestBody @Valid RegistrarAporteRequest request) {

        var aporte = metaPoupancaService.registrarAporte(usuarioId, id, request);
        return ResponseEntity.created(URI.create("/api/metas-poupanca/" + id + "/aportes")).body(aporte);
    }

    @GetMapping("/{id}/aportes")
    public ResponseEntity<PaginaResponse<AporteResponse>> listarAportes(@UsuarioAutenticado UUID usuarioId,
                                                                        @PathVariable UUID id,
                                                                        @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(metaPoupancaService.listarAportes(usuarioId, id, pageable));
    }
}
