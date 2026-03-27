package com.cap.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins — add your production domain here when deploying
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",   // React dev
                "http://localhost:8080"    // Gateway itself (Swagger UI)
//              "http://localhost:4200",   // Angular dev
//              "http://localhost:5173"    // Vite dev
        ));

        // Allowed HTTP methods
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allowed headers
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin"
        ));

        // Allow credentials (cookies, auth headers)
        config.setAllowCredentials(true);

        // How long browser caches preflight response (1 hour)
        config.setMaxAge(3600L);

        // Apply to ALL routes
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}