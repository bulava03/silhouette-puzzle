package com.silhouettepuzzle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Session-based security. Players log in with a form and stay logged in
 * through the session cookie; the game itself can be played anonymously,
 * only results and progress need an account.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /**
     * The security rules.
     *
     * @param http security builder
     * @return the filter chain
     * @throws Exception if the configuration is invalid
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/me", "/api/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/levels", "/api/levels/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/players", "/api/levels/*/attempts", "/api/attempts/*/submit").permitAll()
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().permitAll())
                // The token travels in an XSRF-TOKEN cookie that page scripts
                // read and send back in the X-XSRF-TOKEN header.
                .csrf(csrf -> csrf.spa())
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults())
                // API calls get a plain 401 instead of a redirect to the login page.
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")));
        return http.build();
    }

    /**
     * Password encoder; new passwords are hashed with BCrypt.
     *
     * @return the encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
