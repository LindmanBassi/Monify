package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.enums.TipoReferencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record CriarMetaEconomiaRequest(

        @NotNull(message = "Categoria é obrigatória")
        UUID categoriaId,

        @NotNull(message = "Mês de referência é obrigatório")
        @Schema(type = "string", example = "2026-10")
        YearMonth mesReferencia,

        @NotNull(message = "Tipo de referência é obrigatório")
        TipoReferencia tipoReferencia,

        @NotNull(message = "Valor de economia é obrigatório")
        BigDecimal valorEconomia) {
}
