package com.dailyai.assistant.security;

import com.dailyai.assistant.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerKeyAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validSchedulerKeyGrantsSchedulerRole() throws Exception {
        AppProperties properties = new AppProperties();
        properties.setSchedulerApiKey("scheduler-secret");
        SchedulerKeyAuthenticationFilter filter = new SchedulerKeyAuthenticationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/scheduler/news/refresh");
        request.addHeader(SchedulerKeyAuthenticationFilter.HEADER, "scheduler-secret");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_SCHEDULER");
    }

    @Test
    void wrongOrMissingSchedulerKeyDoesNotAuthenticate() throws Exception {
        AppProperties properties = new AppProperties();
        properties.setSchedulerApiKey("scheduler-secret");
        SchedulerKeyAuthenticationFilter filter = new SchedulerKeyAuthenticationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/scheduler/news/refresh");
        request.addHeader(SchedulerKeyAuthenticationFilter.HEADER, "wrong");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        SecurityContextHolder.clearContext();
        MockHttpServletRequest missing = new MockHttpServletRequest("POST", "/api/scheduler/news/refresh");
        filter.doFilter(missing, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void blankConfiguredSchedulerKeyIsNeverAccepted() throws Exception {
        AppProperties properties = new AppProperties();
        properties.setSchedulerApiKey("");
        SchedulerKeyAuthenticationFilter filter = new SchedulerKeyAuthenticationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/scheduler/news/refresh");
        request.addHeader(SchedulerKeyAuthenticationFilter.HEADER, "");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}