package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.enums.TipoTransacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

public record FiltroTransacoesRequest(

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate inicio,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fim,

        UUID categoriaId,

        TipoTransacao tipo,

        String texto) {

    @Schema(hidden = true)
    @AssertTrue(message = "Data de início não pode ser posterior à data de fim")
    public boolean isPeriodoValido() {
        return inicio == null || fim == null || !inicio.isAfter(fim);
    }
}
