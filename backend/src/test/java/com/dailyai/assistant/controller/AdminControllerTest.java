package com.dailyai.assistant.controller;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.exception.GlobalExceptionHandler;
import com.dailyai.assistant.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setAdminApiKey("secret-key");
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService, properties))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void refreshWithoutKeyIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/news/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithWrongKeyIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/news/refresh").header("X-Admin-Key", "nope"))
                .andExpect(status().isUnauthorized());
    }
}
