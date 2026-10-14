package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.MetaEconomia;
import br.com.bassi.monify.domain.enums.StatusMetaEconomia;
import br.com.bassi.monify.domain.enums.TipoReferencia;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

public record MetaEconomiaResponse(
        UUID id,
        CategoriaResumoResponse categoria,
        @Schema(type = "string", example = "2026-10")
        YearMonth mesReferencia,
        TipoReferencia tipoReferencia,
        UUID orcamentoId,
        BigDecimal valorEconomia,
        BigDecimal valorReferencia,
        BigDecimal valorLimite,
        BigDecimal gasto,
        BigDecimal economiaRealizada,
        StatusMetaEconomia status,
        Instant congeladaEm,
        Instant canceladaEm) {

    // referência, limite e economia realizada só existem depois do congelamento
    public static MetaEconomiaResponse from(MetaEconomia meta, BigDecimal gasto, YearMonth mesAtual) {
        return new MetaEconomiaResponse(
                meta.getId(),
                CategoriaResumoResponse.from(meta.getCategoria()),
                meta.getMesReferencia(),
                meta.getTipoReferencia(),
                meta.getOrcamento() != null ? meta.getOrcamento().getId() : null,
                meta.getValorEconomia(),
                meta.getValorReferencia(),
                meta.getValorLimite(),
                gasto,
                meta.isCongelada() ? meta.calcularEconomiaRealizada(gasto) : null,
                meta.calcularStatus(gasto, mesAtual),
                meta.getCongeladaEm(),
                meta.getCanceladaEm());
    }
}
