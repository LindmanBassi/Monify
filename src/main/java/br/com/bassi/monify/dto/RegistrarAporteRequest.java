package br.com.bassi.monify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarAporteRequest(

        @NotNull(message = "Valor é obrigatório")
        @Schema(description = "Positivo para aporte, negativo para resgate", example = "250.00")
        BigDecimal valor,

        @NotNull(message = "Data é obrigatória")
        LocalDate data,

        @Size(max = 255, message = "Observação deve ter no máximo 255 caracteres")
        String observacao) {
}
