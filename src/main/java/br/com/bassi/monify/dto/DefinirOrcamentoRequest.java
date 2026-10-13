package br.com.bassi.monify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record DefinirOrcamentoRequest(

        @NotNull(message = "Categoria é obrigatória")
        UUID categoriaId,

        @NotNull(message = "Mês de referência é obrigatório")
        @Schema(type = "string", example = "2026-10")
        YearMonth mesReferencia,

        @NotNull(message = "Valor limite é obrigatório")
        BigDecimal valorLimite) {
}
