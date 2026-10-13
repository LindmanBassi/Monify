package br.com.bassi.monify.service;

import br.com.bassi.monify.config.PropriedadesAutenticacao;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String EMISSOR = "monify";

    private static final int BYTES_TOKEN_RENOVACAO = 32;

    private final JwtEncoder jwtEncoder;
    private final PropriedadesAutenticacao propriedades;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public String gerarTokenAcesso(UUID usuarioId) {

        var agora = Instant.now(clock);
        var claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuarioId.toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.duracaoTokenAcesso()))
                .build();
        var cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }

    // token opaco: só o hash é salvo no banco
    public String gerarTokenRenovacao() {

        var bytes = new byte[BYTES_TOKEN_RENOVACAO];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String calcularHash(String tokenRenovacao) {

        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(tokenRenovacao.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public Instant calcularExpiracaoTokenRenovacao() {
        return Instant.now(clock).plus(propriedades.duracaoTokenRenovacao());
    }

    public long duracaoTokenAcessoEmSegundos() {
        return propriedades.duracaoTokenAcesso().toSeconds();
    }
}
