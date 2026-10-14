package br.com.bassi.monify.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// refresh token em cookie httpOnly: o JavaScript do frontend não consegue lê-lo
@Component
@RequiredArgsConstructor
public class CookieTokenRenovacao {

    public static final String NOME = "monify_refresh";

    private static final String CAMINHO = "/api/auth";

    private final PropriedadesAutenticacao propriedades;

    public ResponseCookie criar(String tokenRenovacao) {
        return montar(tokenRenovacao, propriedades.duracaoTokenRenovacao());
    }

    public ResponseCookie remover() {
        return montar("", Duration.ZERO);
    }

    private ResponseCookie montar(String valor, Duration duracao) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true)
                .secure(propriedades.cookieSeguro())
                .sameSite("Strict")
                .path(CAMINHO)
                .maxAge(duracao)
                .build();
    }
}
