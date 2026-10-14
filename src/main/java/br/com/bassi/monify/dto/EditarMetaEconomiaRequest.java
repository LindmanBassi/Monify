package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.enums.TipoReferencia;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EditarMetaEconomiaRequest(

        @NotNull(message = "Tipo de referência é obrigatório")
        TipoReferencia tipoReferencia,

        @NotNull(message = "Valor de economia é obrigatório")
        BigDecimal valorEconomia) {
}
