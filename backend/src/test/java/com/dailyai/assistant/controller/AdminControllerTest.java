package com.dailyai.assistant.controller;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.security.AdminKeyAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class AdminControllerTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validAdminKeyGrantsAdminRole() throws Exception {
        AppProperties properties = new AppProperties();
        properties.setAdminApiKey("secret-key");
        AdminKeyAuthenticationFilter filter = new AdminKeyAuthenticationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/news/refresh");
        request.addHeader(AdminKeyAuthenticationFilter.HEADER, "secret-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void wrongOrMissingKeyDoesNotAuthenticate() throws Exception {
        AppProperties properties = new AppProperties();
        properties.setAdminApiKey("secret-key");
        AdminKeyAuthenticationFilter filter = new AdminKeyAuthenticationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/news/refresh");
        request.addHeader(AdminKeyAuthenticationFilter.HEADER, "nope");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void blankConfiguredKeyIsNeverAccepted() throws Exception {
        AppProperties properties = new AppProperties();
        properties.setAdminApiKey("");
        AdminKeyAuthenticationFilter filter = new AdminKeyAuthenticationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/news/refresh");
        request.addHeader(AdminKeyAuthenticationFilter.HEADER, "");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
