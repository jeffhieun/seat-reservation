package com.linkz.reservation.auth;

import com.linkz.reservation.config.JwtService;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    @Transactional
    public void register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .build();
        
        userRepository.save(user);
    }
    
    public AuthTokens login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return new AuthTokens(
                jwtService.generateAccessToken(email),
                jwtService.generateRefreshToken(email),
                email
        );
    }

    public AuthTokens refresh(String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired token");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));

        return new AuthTokens(
                jwtService.generateAccessToken(user.getEmail()),
                jwtService.generateRefreshToken(user.getEmail()),
                user.getEmail()
        );
    }

    public CurrentUserResponse getCurrentUser(String email, List<String> roles) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getEmail(),
                roles
        );
    }
}
