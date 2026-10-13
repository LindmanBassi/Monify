package br.com.bassi.monify.controller;

import br.com.bassi.monify.config.UsuarioAutenticado;
import br.com.bassi.monify.dto.AlterarLimiteOrcamentoRequest;
import br.com.bassi.monify.dto.DefinirOrcamentoRequest;
import br.com.bassi.monify.dto.OrcamentoResponse;
import br.com.bassi.monify.service.OrcamentoService;
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
@RequestMapping("/api/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    @GetMapping
    public ResponseEntity<List<OrcamentoResponse>> listar(
            @UsuarioAutenticado UUID usuarioId,
            @Parameter(description = "Mês no formato yyyy-MM (padrão: mês atual)", schema = @Schema(type = "string", example = "2026-10"))
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {

        return ResponseEntity.ok(orcamentoService.listar(usuarioId, mes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrcamentoResponse> buscar(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        return ResponseEntity.ok(orcamentoService.buscar(usuarioId, id));
    }

    // 201 quando cria; 200 quando atualiza o orçamento já existente da categoria no mês
    @PostMapping
    public ResponseEntity<OrcamentoResponse> definir(@UsuarioAutenticado UUID usuarioId,
                                                     @RequestBody @Valid DefinirOrcamentoRequest request) {

        var resultado = orcamentoService.definir(usuarioId, request);
        var orcamento = resultado.orcamento();

        if (resultado.criado()) {
            return ResponseEntity.created(URI.create("/api/orcamentos/" + orcamento.id())).body(orcamento);
        }
        return ResponseEntity.ok(orcamento);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> alterarLimite(@UsuarioAutenticado UUID usuarioId,
                                              @PathVariable UUID id,
                                              @RequestBody @Valid AlterarLimiteOrcamentoRequest request) {

        orcamentoService.alterarLimite(usuarioId, id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@UsuarioAutenticado UUID usuarioId, @PathVariable UUID id) {

        orcamentoService.excluir(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
