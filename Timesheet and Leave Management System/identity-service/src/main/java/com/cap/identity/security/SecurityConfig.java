package com.cap.identity.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final InternalAuthFilter internalAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(auth -> auth
                // public — no auth needed
                // Gateway already handles these
                .requestMatchers(
                		"/auth/login",
                        "/auth/signup",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/swagger-ui/**",
                        "/auth/v3/api-docs",
                        "/auth/v3/api-docs/**",
                        "/auth/swagger-ui/**",
                        "/auth/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/health"
                ).permitAll()
                // everything else needs valid headers
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(
                internalAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}