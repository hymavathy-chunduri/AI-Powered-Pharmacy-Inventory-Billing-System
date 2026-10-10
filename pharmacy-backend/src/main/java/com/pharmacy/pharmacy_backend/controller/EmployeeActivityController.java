package com.pharmacy.pharmacy_backend.controller;

import com.pharmacy.pharmacy_backend.service.EmployeeActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class EmployeeActivityController {

    private final EmployeeActivityService activityService;

    @Autowired
    public EmployeeActivityController(EmployeeActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Returns the activity summary for the currently authenticated employee.
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyActivity(Authentication authentication) {
        String employeeId = authentication.getName();
        return ResponseEntity.ok(activityService.getActivitySummary(employeeId));
    }

    /**
     * Admin view: Inspect the activity summary of any employee.
     */
    @GetMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEmployeeActivity(@PathVariable String employeeId) {
        return ResponseEntity.ok(activityService.getActivitySummary(employeeId));
    }
}
