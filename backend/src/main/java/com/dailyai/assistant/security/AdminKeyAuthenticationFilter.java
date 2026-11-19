package com.dailyai.assistant.security;

import com.dailyai.assistant.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Keeps the pre-existing X-Admin-Key mechanism, but maps a valid key to
 * ROLE_ADMIN instead of letting controllers decide security. The key is never
 * logged and is only accepted when configured to a non-blank value.
 */
@Component
public class AdminKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Admin-Key";

    private final AppProperties properties;

    public AdminKeyAuthenticationFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String expected = properties.getAdminApiKey();
        String provided = request.getHeader(HEADER);

        if (expected != null && !expected.isBlank() && provided != null
                && SecurityContextHolder.getContext().getAuthentication() == null
                && constantTimeEquals(expected, provided)) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "admin-key", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String expected, String provided) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }
}
