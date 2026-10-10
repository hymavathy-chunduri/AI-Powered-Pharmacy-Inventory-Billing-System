package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.dto.LoginRequestDto;
import com.pharmacy.pharmacy_backend.entity.Employee;
import com.pharmacy.pharmacy_backend.repository.EmployeeRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Employee testPharmacist;

    @BeforeEach
    void setUp() {
        if (!employeeRepository.existsByEmployeeIdIgnoreCase("EMP-TEST-01")) {
            testPharmacist = new Employee(
                    "EMP-TEST-01",
                    "Test Pharmacist",
                    "pharmacist.test@pharmacare.com",
                    passwordEncoder.encode("Secret@123"),
                    "PHARMACIST"
            );
            testPharmacist.setActive(true);
            employeeRepository.save(testPharmacist);
        } else {
            testPharmacist = employeeRepository.findByEmployeeIdIgnoreCase("EMP-TEST-01").get();
        }
    }

    @Test
    void testUnauthenticatedAccessBlocked() throws Exception {
        mockMvc.perform(get("/api/medicines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    void testLoginSuccessWithEmployeeId() throws Exception {
        LoginRequestDto req = new LoginRequestDto("EMP-TEST-01", "Secret@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP-TEST-01"))
                .andExpect(jsonPath("$.role").value("PHARMACIST"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()) // passwordHash must never be exposed
                .andExpect(cookie().exists("JSESSIONID"));
    }

    @Test
    void testLoginSuccessWithEmail() throws Exception {
        LoginRequestDto req = new LoginRequestDto("pharmacist.test@pharmacare.com", "Secret@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP-TEST-01"))
                .andExpect(jsonPath("$.email").value("pharmacist.test@pharmacare.com"));
    }

    @Test
    void testLoginInvalidPasswordFails() throws Exception {
        LoginRequestDto req = new LoginRequestDto("EMP-TEST-01", "WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void testLoginNonExistentEmployeeFails() throws Exception {
        LoginRequestDto req = new LoginRequestDto("NON_EXISTENT_ID", "SomePassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLoginInactiveEmployeeRejected() throws Exception {
        Employee inactive = new Employee(
                "EMP-INACTIVE",
                "Inactive Staff",
                "inactive@pharmacare.com",
                passwordEncoder.encode("Secret@123"),
                "CASHIER"
        );
        inactive.setActive(false);
        employeeRepository.save(inactive);

        LoginRequestDto req = new LoginRequestDto("EMP-INACTIVE", "Secret@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("inactive")));
    }

    @Test
    void testSessionLifecycle() throws Exception {
        // 1. Login
        LoginRequestDto req = new LoginRequestDto("EMP-TEST-01", "Secret@123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        Cookie sessionCookie = loginResult.getResponse().getCookie("JSESSIONID");

        // 2. Access /api/auth/me with session
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP-TEST-01"));

        // 3. Access protected pharmacy endpoint with session
        mockMvc.perform(get("/api/medicines").session(session))
                .andExpect(status().isOk());

        // 4. Logout
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // 5. Verify /api/auth/me returns 401 after logout
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
