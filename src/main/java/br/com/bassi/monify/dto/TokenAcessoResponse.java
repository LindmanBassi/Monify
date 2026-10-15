package br.com.bassi.monify.dto;

public record TokenAcessoResponse(String tokenAcesso, String tipo, long expiraEmSegundos) {

    public static TokenAcessoResponse bearer(String tokenAcesso, long expiraEmSegundos) {
        return new TokenAcessoResponse(tokenAcesso, "Bearer", expiraEmSegundos);
    }
}
