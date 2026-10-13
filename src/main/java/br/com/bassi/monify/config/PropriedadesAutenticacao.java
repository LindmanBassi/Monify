package br.com.bassi.monify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "monify.autenticacao")
public record PropriedadesAutenticacao(
        String segredoJwt,
        Duration duracaoTokenAcesso,
        Duration duracaoTokenRenovacao) {
}
