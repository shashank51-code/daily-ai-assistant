package com.dailyai.assistant.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The logout cookie must expire the session cookie with the same attributes
 * the container used to create it (server.servlet.session.cookie.*).
 */
class AuthControllerSessionCookieTest {

    @Test
    void expiredCookieMatchesSessionAttributes() {
        ResponseCookie cookie = AuthController.expiredSessionCookie(true, "lax");

        assertThat(cookie.getName()).isEqualTo("DAILYAI_SESSION");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    void sameSiteIsCanonicalisedAndNullWhenUnset() {
        assertThat(AuthController.expiredSessionCookie(false, "NONE").getSameSite()).isEqualTo("None");
        assertThat(AuthController.expiredSessionCookie(false, "Strict").getSameSite()).isEqualTo("Strict");
        assertThat(AuthController.expiredSessionCookie(false, null).getSameSite()).isNull();
        assertThat(AuthController.expiredSessionCookie(false, "  ").getSameSite()).isNull();
    }

    @Test
    void insecureCookieStaysInsecureForLocalDevelopment() {
        ResponseCookie cookie = AuthController.expiredSessionCookie(false, "lax");

        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.isHttpOnly()).isTrue();
    }
}
