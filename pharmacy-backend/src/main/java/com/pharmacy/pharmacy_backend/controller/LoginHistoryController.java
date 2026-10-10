package com.pharmacy.pharmacy_backend.controller;

import com.pharmacy.pharmacy_backend.service.LoginHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/login-history")
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @Autowired
    public LoginHistoryController(LoginHistoryService loginHistoryService) {
        this.loginHistoryService = loginHistoryService;
    }

    /**
     * Admin view: Query login events across the entire organization with filters & pagination.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOrganizationLoginHistory(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> history = loginHistoryService.getHistory(
                employeeId, eventType, status, startDate, endDate, page, size
        );
        return ResponseEntity.ok(history);
    }

    /**
     * Employee view: Returns only the authenticated employee's login history.
     * Guaranteed never to use or trust a client-supplied employee ID.
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyLoginHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String employeeId = authentication.getName();
        Map<String, Object> history = loginHistoryService.getEmployeeHistory(employeeId, page, size);
        return ResponseEntity.ok(history);
    }
}
