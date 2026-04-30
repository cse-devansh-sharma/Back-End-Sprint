package com.cap.leave.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/business")
        public void throwBusiness() {
            throw new BusinessRuleException("Business rule violated");
        }

        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Resource not found");
        }
        
        @GetMapping("/test/leave-not-found")
        public void throwLeaveNotFound() {
            throw new LeaveNotFoundException("Leave not found");
        }
    }

    @Test
    void handleBusinessRuleException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Business rule violated"));
    }

    @Test
    void handleResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    void handleLeaveNotFoundException() throws Exception {
        mockMvc.perform(get("/test/leave-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Leave not found"));
    }
}
