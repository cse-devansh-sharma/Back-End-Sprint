package com.cap.timesheet.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ConfigTests {

    @Test
    void appConfig_Initialization() {
        AppConfig appConfig = new AppConfig();
        assertNotNull(appConfig);
    }

    @Test
    void rabbitMQConfig_Beans() {
        RabbitMQConfig config = new RabbitMQConfig();
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        
        Jackson2JsonMessageConverter converter = config.messageConverter();
        RabbitTemplate template = config.rabbitTemplate(connectionFactory);
        
        assertNotNull(converter);
        assertNotNull(template);
    }

    @Test
    void swaggerConfig_Bean() {
        SwaggerConfig config = new SwaggerConfig();
        OpenAPI openAPI = config.customOpenAPI();
        
        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("TimeSheet Service API", openAPI.getInfo().getTitle());
    }

    private void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
