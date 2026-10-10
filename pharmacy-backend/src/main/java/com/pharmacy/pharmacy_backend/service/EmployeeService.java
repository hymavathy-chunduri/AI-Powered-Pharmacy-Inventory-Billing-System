package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.dto.CreateEmployeeRequestDto;
import com.pharmacy.pharmacy_backend.dto.EmployeeResponseDto;
import com.pharmacy.pharmacy_backend.dto.UpdateEmployeeRequestDto;
import com.pharmacy.pharmacy_backend.entity.Employee;
import com.pharmacy.pharmacy_backend.exception.DuplicateResourceException;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<EmployeeResponseDto> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(EmployeeResponseDto::new)
                .collect(Collectors.toList());
    }

    public EmployeeResponseDto getEmployeeById(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        return new EmployeeResponseDto(employee);
    }

    public EmployeeResponseDto createEmployee(CreateEmployeeRequestDto dto) {
        String empId = dto.getEmployeeId() != null ? dto.getEmployeeId().trim() : "";
        String email = dto.getEmail() != null ? dto.getEmail().toLowerCase().trim() : "";

        if (employeeRepository.existsByEmployeeIdIgnoreCase(empId)) {
            throw new DuplicateResourceException("Employee ID '" + empId + "' is already registered.");
        }
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email '" + email + "' is already registered.");
        }

        String role = dto.getRole() != null ? dto.getRole().toUpperCase().trim() : "PHARMACIST";
        if (!List.of("ADMIN", "PHARMACIST", "CASHIER").contains(role)) {
            role = "PHARMACIST";
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        Employee employee = new Employee(empId, dto.getFullName().trim(), email, encodedPassword, role);
        Employee saved = employeeRepository.save(employee);
        log.info("Employee created successfully: employeeId={}, role={}", empId, role);
        return new EmployeeResponseDto(saved);
    }

    public EmployeeResponseDto updateEmployee(String id, UpdateEmployeeRequestDto dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            employee.setFullName(dto.getFullName().trim());
        }
        if (dto.getRole() != null && !dto.getRole().trim().isEmpty()) {
            String role = dto.getRole().toUpperCase().trim();
            if (List.of("ADMIN", "PHARMACIST", "CASHIER").contains(role)) {
                employee.setRole(role);
            }
        }
        if (dto.getActive() != null) {
            employee.setActive(dto.getActive());
        }
        employee.setUpdatedAt(LocalDateTime.now());
        Employee saved = employeeRepository.save(employee);
        return new EmployeeResponseDto(saved);
    }

    public EmployeeResponseDto setEmployeeActiveStatus(String id, boolean active) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        employee.setActive(active);
        employee.setUpdatedAt(LocalDateTime.now());
        Employee saved = employeeRepository.save(employee);
        log.info("Employee status updated: employeeId={}, active={}", employee.getEmployeeId(), active);
        return new EmployeeResponseDto(saved);
    }

    public void resetPassword(String id, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employee.setUpdatedAt(LocalDateTime.now());
        employeeRepository.save(employee);
        log.info("Password reset successfully for employeeId={}", employee.getEmployeeId());
    }

    /**
     * One-time idempotent bootstrap for initial administrator account.
     * Triggers on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapDefaultAdmin() {
        try {
            if (employeeRepository.countByRole("ADMIN") == 0) {
                String adminEmail = System.getenv().getOrDefault("ADMIN_BOOTSTRAP_EMAIL", "admin@pharmacare.com").toLowerCase().trim();
                String adminPassword = System.getenv().getOrDefault("ADMIN_BOOTSTRAP_PASSWORD", "Admin@123456");
                String adminId = System.getenv().getOrDefault("ADMIN_BOOTSTRAP_ID", "EMP-001");
                String adminName = System.getenv().getOrDefault("ADMIN_BOOTSTRAP_NAME", "System Administrator");

                if (!employeeRepository.existsByEmailIgnoreCase(adminEmail) && !employeeRepository.existsByEmployeeIdIgnoreCase(adminId)) {
                    Employee admin = new Employee(adminId, adminName, adminEmail, passwordEncoder.encode(adminPassword), "ADMIN");
                    admin.setActive(true);
                    employeeRepository.save(admin);
                    log.info("Initial administrator account provisioned: employeeId={}, email={}", adminId, adminEmail);
                }
            } else {
                log.info("Administrator accounts already present in MongoDB. Bootstrap skipped.");
            }
        } catch (Exception e) {
            log.warn("Bootstrap admin check encountered an error: {}", e.getMessage());
        }
    }
}
