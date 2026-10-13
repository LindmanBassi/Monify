package br.com.bassi.monify.dto;

public record TokensResponse(
        String tokenAcesso,
        String tokenRenovacao,
        String tipo,
        long expiraEmSegundos) {
}
