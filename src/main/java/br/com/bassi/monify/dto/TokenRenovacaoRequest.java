package br.com.bassi.monify.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRenovacaoRequest(

        @NotBlank(message = "Token de renovação é obrigatório")
        String tokenRenovacao) {
}
