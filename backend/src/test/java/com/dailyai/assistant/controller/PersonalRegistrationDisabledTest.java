package com.dailyai.assistant.controller;

import com.dailyai.assistant.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.auth.registration-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalRegistrationDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void disabledRegistrationDoesNotCreateAnotherUser() throws Exception {
        long usersBefore = userRepository.count();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Not Allowed","email":"not-allowed@example.com",
                                 "password":"StrongPass1!","confirmPassword":"StrongPass1!"}
                                """))
                .andExpect(status().isNotFound());

        assertThat(userRepository.count()).isEqualTo(usersBefore);
    }
}
