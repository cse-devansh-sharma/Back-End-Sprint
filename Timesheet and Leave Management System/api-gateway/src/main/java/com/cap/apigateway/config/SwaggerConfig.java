package com.cap.apigateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();

        Set<SwaggerUrl> urls = new LinkedHashSet<>();

        // ✅ URL here is what the BROWSER fetches via the gateway
        // Gateway route strips /auth prefix → identity-service receives /v3/api-docs
        // Same logic for all services
        urls.add(makeUrl("Identity Service",  "/auth/v3/api-docs"));
        urls.add(makeUrl("Timesheet Service", "/timesheet/v3/api-docs"));
        urls.add(makeUrl("Leave Service",     "/leave/v3/api-docs"));
        urls.add(makeUrl("Admin Service",     "/admin/v3/api-docs"));
        urls.add(makeUrl("Notification Service", "/notification/v3/api-docs"));

        config.setUrls(urls);
        config.setDisplayRequestDuration(true);
        config.setTryItOutEnabled(true);

        return config;
    }

    private SwaggerUrl makeUrl(String name, String url) {
        SwaggerUrl swaggerUrl = new SwaggerUrl();
        swaggerUrl.setName(name);
        swaggerUrl.setUrl(url);
        return swaggerUrl;
    }
}