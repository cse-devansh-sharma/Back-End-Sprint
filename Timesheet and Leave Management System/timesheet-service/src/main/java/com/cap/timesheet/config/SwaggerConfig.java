package com.cap.timesheet.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
        		.servers(List.of(new Server().url("http://localhost:8080")))
                .info(new Info()
                        .title("TimeSheet Service API")
                        .version("1.0")
                        .description("Timesheet Service APIs"))

                // 🔐 JWT Configuration
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))

                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}