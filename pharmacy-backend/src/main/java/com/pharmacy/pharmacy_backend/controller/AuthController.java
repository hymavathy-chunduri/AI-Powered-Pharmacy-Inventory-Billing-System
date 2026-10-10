package com.pharmacy.pharmacy_backend.controller;

import com.pharmacy.pharmacy_backend.dto.EmployeeResponseDto;
import com.pharmacy.pharmacy_backend.dto.LoginRequestDto;
import com.pharmacy.pharmacy_backend.dto.RegisterEmployeeRequestDto;
import com.pharmacy.pharmacy_backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<EmployeeResponseDto> register(@Valid @RequestBody RegisterEmployeeRequestDto request) {
        EmployeeResponseDto response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<EmployeeResponseDto> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        EmployeeResponseDto response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentEmployee() {
        try {
            EmployeeResponseDto current = authService.getCurrentEmployee();
            return ResponseEntity.ok(current);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("status", "UNAUTHENTICATED", "message", "No active employee session."));
        }
    }
}
