package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.enums.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarCategoriaRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 50, message = "Nome deve ter no máximo 50 caracteres")
        String nome,

        @NotNull(message = "Tipo é obrigatório")
        TipoTransacao tipo,

        @NotBlank(message = "Cor é obrigatória")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato hexadecimal #RRGGBB")
        String cor) {
}
