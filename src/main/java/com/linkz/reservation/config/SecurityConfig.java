package com.linkz.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/login",
        "/actuator/health",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**"
    };
    
    @Value("${security.remember-me.key}")
    private String rememberMeKey;
    
    @Value("${security.remember-me.duration-days:90}")
    private int rememberMeDurationDays;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        authProvider.setHideUserNotFoundExceptions(true);
        return authProvider;
    }
    
    @Bean
    public RememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
        int durationSeconds = rememberMeDurationDays * 24 * 60 * 60;
        
        TokenBasedRememberMeServices rememberMeServices = new TokenBasedRememberMeServices(
            rememberMeKey,
            userDetailsService
        );
        rememberMeServices.setTokenValiditySeconds(durationSeconds);
        rememberMeServices.setParameter("remember-me");
        return rememberMeServices;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RememberMeServices rememberMeServices) throws Exception {
        
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .invalidSessionUrl("/api/auth/login")
            )
            .rememberMe(rememberMe -> rememberMe
                .rememberMeServices(rememberMeServices)
                .key(rememberMeKey)
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/api/auth/login")
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
        
        return http.build();
    }
}

