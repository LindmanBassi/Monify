package br.com.bassi.monify.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ResumoMensalResponse(
        @Schema(type = "string", example = "2026-10")
        YearMonth mes,
        BigDecimal receitas,
        BigDecimal despesas,
        BigDecimal saldo) {

    public static ResumoMensalResponse of(YearMonth mes, BigDecimal receitas, BigDecimal despesas) {
        return new ResumoMensalResponse(mes, receitas, despesas, receitas.subtract(despesas));
    }
}
