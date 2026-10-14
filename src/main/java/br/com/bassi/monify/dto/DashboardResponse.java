package br.com.bassi.monify.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.YearMonth;
import java.util.List;

public record DashboardResponse(
        @Schema(type = "string", example = "2026-10")
        YearMonth mes,
        ResumoMensalResponse resumo,
        VariacaoMensalResponse comparacaoMesAnterior,
        List<TotalCategoriaResponse> gastosPorCategoria,
        List<TotalCategoriaResponse> receitasPorCategoria,
        List<ResumoMensalResponse> evolucaoMensal) {
}
