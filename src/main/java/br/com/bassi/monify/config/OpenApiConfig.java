package br.com.bassi.monify.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearer-jwt";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Monify API")
                        .description("API de gestão financeira pessoal: categorias, transações, orçamentos, metas e dashboard.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Informe o tokenAcesso retornado pelo login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}
