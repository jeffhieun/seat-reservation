package com.linkz.reservation.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import com.linkz.reservation.commons.exception.ErrorResponse;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody LoginRequest request) {
        try {
            authService.register(request.email(), request.password());
            log.info("User registered successfully: {}", request.email());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            HttpStatus status = e.getMessage() != null && e.getMessage().toLowerCase().contains("already")
                    ? HttpStatus.CONFLICT
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(errorResponse(status, e.getMessage(), "/api/auth/register"));
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "/api/auth/register"));
        }
    }

    // Support form-urlencoded requests (e.g. from HTML forms or some Swagger UI configurations)
    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> registerForm(@RequestParam("email") String email,
                                             @RequestParam("password") String password) {
        try {
            authService.register(email, password);
            log.info("User registered successfully: {}", email);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            HttpStatus status = e.getMessage() != null && e.getMessage().toLowerCase().contains("already")
                    ? HttpStatus.CONFLICT
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(errorResponse(status, e.getMessage(), "/api/auth/register"));
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "/api/auth/register"));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.debug("Login attempt for email: {}", request.email());
            AuthTokens tokens = authService.login(request.email(), request.password());
            log.info("User logged in successfully: {}", request.email());
            return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken(), request.email()));
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), "/api/auth/login"));
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "/api/auth/login"));
        }
    }

    // Support form-urlencoded login
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> loginForm(@RequestParam("email") String email,
                                       @RequestParam("password") String password) {
        try {
            log.debug("Login attempt for email: {}", email);
            AuthTokens tokens = authService.login(email, password);
            log.info("User logged in successfully: {}", email);
            return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken(), email));
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), "/api/auth/login"));
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "/api/auth/login"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokens tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken(), tokens.email()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BadCredentialsException("Invalid or expired token");
        }

        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }

    private ErrorResponse errorResponse(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now().toString(), status.value(), status.getReasonPhrase(), message, path);
    }
}
