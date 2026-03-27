package com.cap.admin.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
                .requestMatchers(
                		"/admin/config/public",
                        "/swagger-ui/**",
                        "/admin/v3/api-docs/**",
                        "/admin/v3/api-docs",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/health"
                ).permitAll()
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
}