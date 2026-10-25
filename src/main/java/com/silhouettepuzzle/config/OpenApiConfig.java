package com.silhouettepuzzle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * The OpenAPI description, served at {@code /v3/api-docs} and browsable at
 * {@code /swagger-ui.html}.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String SESSION = "session";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Silhouette Puzzle API")
                        .version("v1")
                        .description("""
                                Picture-building puzzle with server-side geometry checking.

                                Log in through `POST /login` (form fields `username` and `password`); \
                                the session cookie then authenticates further calls. Every POST needs \
                                the CSRF token from the `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` \
                                header, which Swagger UI sends on its own.""")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components().addSecuritySchemes(SESSION, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID")
                        .description("Session cookie set by POST /login")));
    }
}
