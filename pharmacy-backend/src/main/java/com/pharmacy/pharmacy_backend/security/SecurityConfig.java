package com.pharmacy.pharmacy_backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:https://hymavathy-chunduri.github.io,http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"status\":\"UNAUTHORIZED\",\"message\":\"Authentication required. Please log in with your employee credentials.\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"status\":\"FORBIDDEN\",\"message\":\"Access denied: insufficient permissions for this operation.\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public health and auth endpoints
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout", "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").permitAll()

                // Admin-only employee management
                .requestMatchers("/api/employees/**").hasRole("ADMIN")

                // Login History: /my accessible to any authenticated employee, / (all) restricted to ADMIN
                .requestMatchers("/api/login-history/my").hasAnyRole("ADMIN", "PHARMACIST", "CASHIER")
                .requestMatchers("/api/login-history/**").hasRole("ADMIN")

                // Employee Activity: /my accessible to any authenticated employee, /{employeeId} restricted to ADMIN
                .requestMatchers("/api/activity/my").hasAnyRole("ADMIN", "PHARMACIST", "CASHIER")
                .requestMatchers("/api/activity/**").hasRole("ADMIN")

                // Purchases and Suppliers: Admin and Pharmacist
                .requestMatchers("/api/purchases/**", "/api/suppliers/**").hasAnyRole("ADMIN", "PHARMACIST")

                // Reports: Admin and Pharmacist
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "PHARMACIST", "CASHIER")

                // AI Demand Predictions: Admin and Pharmacist
                .requestMatchers("/api/predictions/**").hasAnyRole("ADMIN", "PHARMACIST")

                // Medicines and Categories: Writes restricted to Admin/Pharmacist, reads permitted for all roles
                .requestMatchers(HttpMethod.POST, "/api/medicines/**", "/api/categories/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.PUT, "/api/medicines/**", "/api/categories/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/api/medicines/**", "/api/categories/**").hasAnyRole("ADMIN", "PHARMACIST")

                // All other API endpoints (Billing, Customers, My Bills): All authenticated employees (Admin, Pharmacist, Cashier)
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "PHARMACIST", "CASHIER")

                // Any other request requires authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
