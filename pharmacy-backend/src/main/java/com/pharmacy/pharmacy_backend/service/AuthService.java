package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.dto.EmployeeResponseDto;
import com.pharmacy.pharmacy_backend.dto.LoginRequestDto;
import com.pharmacy.pharmacy_backend.dto.RegisterEmployeeRequestDto;
import com.pharmacy.pharmacy_backend.entity.Employee;
import com.pharmacy.pharmacy_backend.exception.DuplicateResourceException;
import com.pharmacy.pharmacy_backend.repository.EmployeeRepository;
import com.pharmacy.pharmacy_backend.security.LoginRateLimiter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;
    private final LoginHistoryService loginHistoryService;

    @Value("${employee.registration.code:1234}")
    private String configuredRegistrationCode;

    @Autowired
    public AuthService(EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder,
                       LoginRateLimiter loginRateLimiter,
                       LoginHistoryService loginHistoryService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginRateLimiter = loginRateLimiter;
        this.loginHistoryService = loginHistoryService;
    }

    /**
     * Self-registration for new pharmacy employees.
     * Validates employee registration code (must equal '1234').
     * Assigns least-privileged role 'CASHIER' permanently.
     */
    public EmployeeResponseDto register(RegisterEmployeeRequestDto request) {
        String code = request.getEmployeeCode() != null ? request.getEmployeeCode().trim() : "";
        if (!configuredRegistrationCode.trim().equals(code)) {
            throw new BadCredentialsException("Invalid employee registration code. Authorization required.");
        }

        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match.");
        }

        String empId = request.getEmployeeId() != null ? request.getEmployeeId().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";

        if (employeeRepository.existsByEmployeeIdIgnoreCase(empId)) {
            throw new DuplicateResourceException("Employee ID '" + empId + "' is already registered.");
        }
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email '" + email + "' is already registered.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Employee employee = new Employee(
                empId,
                request.getFirstName() != null ? request.getFirstName().trim() : "",
                request.getLastName() != null ? request.getLastName().trim() : "",
                email,
                request.getPhone() != null ? request.getPhone().trim() : "",
                encodedPassword,
                "CASHIER" // Least privileged role by default for self-registration
        );
        employee.setActive(true);

        Employee saved = employeeRepository.save(employee);
        log.info("New employee self-registered successfully: employeeId={}, role={}", saved.getEmployeeId(), saved.getRole());
        return new EmployeeResponseDto(saved);
    }

    public EmployeeResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String identifier = request.getIdentifier() != null ? request.getIdentifier().trim() : "";

        if (loginRateLimiter.isBlocked(identifier)) {
            long remaining = loginRateLimiter.getRemainingLockoutSeconds(identifier);
            loginHistoryService.recordEvent(identifier, null, "LOGIN_FAILURE", "FAILURE", "ACCOUNT_LOCKED", null, httpRequest);
            throw new BadCredentialsException("Account temporarily locked due to multiple failed login attempts. Please try again in " + remaining + " seconds.");
        }

        Optional<Employee> empOpt = employeeRepository.findByEmployeeIdIgnoreCase(identifier);
        if (empOpt.isEmpty()) {
            empOpt = employeeRepository.findByEmailIgnoreCase(identifier.toLowerCase());
        }

        if (empOpt.isEmpty()) {
            loginRateLimiter.recordFailure(identifier);
            loginHistoryService.recordEvent(identifier, null, "LOGIN_FAILURE", "FAILURE", "EMPLOYEE_NOT_FOUND", null, httpRequest);
            throw new BadCredentialsException("Invalid employee ID/email or password.");
        }

        Employee emp = empOpt.get();

        if (!emp.isActive()) {
            loginHistoryService.recordEvent(emp.getEmployeeId(), emp.getFullName(), "LOGIN_FAILURE", "FAILURE", "ACCOUNT_INACTIVE", null, httpRequest);
            throw new BadCredentialsException("Employee account is inactive. Please contact an administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), emp.getPasswordHash())) {
            loginRateLimiter.recordFailure(identifier);
            loginHistoryService.recordEvent(emp.getEmployeeId(), emp.getFullName(), "LOGIN_FAILURE", "FAILURE", "INVALID_CREDENTIALS", null, httpRequest);
            throw new BadCredentialsException("Invalid employee ID/email or password.");
        }

        // Authentication successful
        loginRateLimiter.recordSuccess(identifier);

        emp.setLastLoginAt(LocalDateTime.now());
        employeeRepository.save(emp);

        // Establish Spring Security Context in HTTP Session
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                emp.getEmployeeId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + emp.getRole()))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        httpResponse.addCookie(sessionCookie);

        // Record audit event in separate database
        loginHistoryService.recordEvent(emp.getEmployeeId(), emp.getFullName(), "LOGIN_SUCCESS", "SUCCESS", null, session.getId(), httpRequest);

        log.info("Employee logged in successfully: employeeId={}, role={}", emp.getEmployeeId(), emp.getRole());
        return new EmployeeResponseDto(emp);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String empId = null;
        String empName = null;
        String sessionId = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            empId = auth.getName();
            Optional<Employee> empOpt = employeeRepository.findByEmployeeIdIgnoreCase(empId);
            if (empOpt.isPresent()) {
                empName = empOpt.get().getFullName();
            }
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionId = session.getId();
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        if (empId != null) {
            loginHistoryService.recordEvent(empId, empName, "LOGOUT", "SUCCESS", null, sessionId, request);
        }
    }

    public EmployeeResponseDto getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BadCredentialsException("Unauthenticated");
        }

        String employeeId = auth.getName();
        Employee emp = employeeRepository.findByEmployeeIdIgnoreCase(employeeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated employee not found"));

        if (!emp.isActive()) {
            throw new BadCredentialsException("Employee account is inactive.");
        }

        return new EmployeeResponseDto(emp);
    }
}
