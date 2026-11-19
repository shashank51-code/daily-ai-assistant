package com.dailyai.assistant.controller;

import com.dailyai.assistant.dto.AuthUserDto;
import com.dailyai.assistant.dto.LoginRequest;
import com.dailyai.assistant.dto.RegisterRequest;
import com.dailyai.assistant.entity.User;
import com.dailyai.assistant.exception.TooManyRequestsException;
import com.dailyai.assistant.exception.UnauthorizedException;
import com.dailyai.assistant.security.AppUserPrincipal;
import com.dailyai.assistant.security.LoginAttemptService;
import com.dailyai.assistant.service.AuthService;
import com.dailyai.assistant.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_COOKIE = "DAILYAI_SESSION";

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService,
                          CurrentUserService currentUserService,
                          AuthenticationManager authenticationManager,
                          LoginAttemptService loginAttemptService,
                          SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken(), "headerName", csrfToken.getHeaderName());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthUserDto> register(@Valid @RequestBody RegisterRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        User user = authService.register(request);
        AppUserPrincipal principal = new AppUserPrincipal(user);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        establishSession(authentication, httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.toDto(user));
    }

    @PostMapping("/login")
    public AuthUserDto login(@Valid @RequestBody LoginRequest request,
                             HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) {
        String email = AuthService.normalizeEmail(request.email());
        String clientAddress = httpRequest.getRemoteAddr();
        if (loginAttemptService.isBlocked(email, clientAddress)) {
            throw new TooManyRequestsException("Too many login attempts. Please try again later.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(email, clientAddress);
            // Deliberately identical for unknown email, wrong password and disabled
            // accounts so the response cannot be used to enumerate accounts.
            throw new UnauthorizedException("Invalid email or password");
        }

        loginAttemptService.reset(email, clientAddress);
        establishSession(authentication, httpRequest, httpResponse);
        return authService.toDto(currentUserService.getCurrentUser());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        ResponseCookie expired = ResponseCookie.from(SESSION_COOKIE, "")
                .path("/")
                .httpOnly(true)
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .build();
    }

    @GetMapping("/me")
    public AuthUserDto me() {
        return authService.toDto(currentUserService.getCurrentUser());
    }

    private void establishSession(Authentication authentication, HttpServletRequest request,
                                  HttpServletResponse response) {
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
