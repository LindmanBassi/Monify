package br.com.bassi.monify.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AlterarLimiteOrcamentoRequest(

        @NotNull(message = "Valor limite é obrigatório")
        BigDecimal valorLimite) {
}
