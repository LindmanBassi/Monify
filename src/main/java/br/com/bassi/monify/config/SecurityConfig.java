package br.com.bassi.monify.config;

import br.com.bassi.monify.handler.NaoAutenticadoEntryPoint;
import br.com.bassi.monify.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(PropriedadesAutenticacao.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final int TAMANHO_MINIMO_SEGREDO_JWT = 32;

    private static final String[] ROTAS_PUBLICAS = {
            "/api/auth/cadastro",
            "/api/auth/login",
            "/api/auth/renovar",
            "/api/auth/logout"
    };

    private final PropriedadesAutenticacao propriedades;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, NaoAutenticadoEntryPoint naoAutenticadoEntryPoint) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, ROTAS_PUBLICAS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(naoAutenticadoEntryPoint))
                .exceptionHandling(excecoes -> excecoes.authenticationEntryPoint(naoAutenticadoEntryPoint))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(chaveJwt())
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        var decoder = NimbusJwtDecoder.withSecretKey(chaveJwt())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.EMISSOR));
        return decoder;
    }

    private SecretKey chaveJwt() {
        var segredo = propriedades.segredoJwt().getBytes(StandardCharsets.UTF_8);

        if (segredo.length < TAMANHO_MINIMO_SEGREDO_JWT) {
            throw new IllegalStateException("monify.autenticacao.segredo-jwt deve ter pelo menos 32 bytes");
        }

        return new SecretKeySpec(segredo, "HmacSHA256");
    }
}
