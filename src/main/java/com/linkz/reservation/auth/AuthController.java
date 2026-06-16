package com.linkz.reservation.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.debug("Login attempt for email: {}", loginRequest.email());
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.email(),
                    loginRequest.password()
                )
            );
            
            log.info("User logged in successfully: {}", loginRequest.email());
            
            return ResponseEntity.ok(new LoginResponse("Login successful"));
            
        } catch (UsernameNotFoundException e) {
            log.warn("Login attempt with non-existent user");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            
        } catch (BadCredentialsException e) {
            log.warn("Login attempt with invalid credentials for email: {}", 
                loginRequest.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            
        } catch (AuthenticationException e) {
            log.warn("Authentication error: {} for email: {}", 
                e.getClass().getSimpleName(), loginRequest.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            
        } catch (Exception e) {
            log.error("Unexpected error during authentication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}



