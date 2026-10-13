package br.com.bassi.monify.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EditarTransacaoRequest(

        @NotNull(message = "Categoria é obrigatória")
        UUID categoriaId,

        @NotNull(message = "Valor é obrigatório")
        BigDecimal valor,

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
        String descricao,

        @NotNull(message = "Data da transação é obrigatória")
        LocalDate dataOcorrencia) {
}
