package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.Orcamento;
import br.com.bassi.monify.domain.enums.StatusOrcamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        CategoriaResumoResponse categoria,
        @Schema(type = "string", example = "2026-10")
        YearMonth mesReferencia,
        BigDecimal valorLimite,
        BigDecimal gasto,
        BigDecimal restante,
        BigDecimal percentual,
        StatusOrcamento status) {

    // gasto calculado no banco; restante, percentual e status calculados pela entidade
    public static OrcamentoResponse from(Orcamento orcamento, BigDecimal gasto) {
        return new OrcamentoResponse(
                orcamento.getId(),
                CategoriaResumoResponse.from(orcamento.getCategoria()),
                orcamento.getMesReferencia(),
                orcamento.getValorLimite(),
                gasto,
                orcamento.calcularRestante(gasto),
                orcamento.calcularPercentual(gasto),
                orcamento.calcularStatus(gasto));
    }
}
