package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.dto.CreateEmployeeRequestDto;
import com.pharmacy.pharmacy_backend.dto.ResetPasswordRequestDto;
import com.pharmacy.pharmacy_backend.dto.UpdateEmployeeRequestDto;
import com.pharmacy.pharmacy_backend.entity.Employee;
import com.pharmacy.pharmacy_backend.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminCanListEmployees() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    void testPharmacistForbiddenFromEmployeeManagement() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    void testCashierForbiddenFromEmployeeManagement() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCreateEmployeeAndDuplicateRejection() throws Exception {
        long ts = System.currentTimeMillis();
        String empId = "EMP-" + ts;
        String email = "emp" + ts + "@pharmacare.com";

        CreateEmployeeRequestDto req = new CreateEmployeeRequestDto(
                empId,
                "New Staff " + ts,
                email,
                "StrongPass@123",
                "PHARMACIST"
        );

        // 1. Create successfully
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(empId))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("PHARMACIST"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // 2. Reject duplicate employeeId
        CreateEmployeeRequestDto dupId = new CreateEmployeeRequestDto(
                empId,
                "Another Name",
                "different" + ts + "@pharmacare.com",
                "StrongPass@123",
                "CASHIER"
        );
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("DUPLICATE_RESOURCE"));

        // 3. Reject duplicate email
        CreateEmployeeRequestDto dupEmail = new CreateEmployeeRequestDto(
                "EMP-DIFF-" + ts,
                "Another Name",
                email,
                "StrongPass@123",
                "CASHIER"
        );
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUpdateAndDeactivateEmployee() throws Exception {
        long ts = System.currentTimeMillis();
        Employee emp = new Employee("EMP-UPD-" + ts, "Update Test", "upd" + ts + "@pharmacare.com", "hash", "CASHIER");
        emp = employeeRepository.save(emp);

        // Update details
        UpdateEmployeeRequestDto updateReq = new UpdateEmployeeRequestDto("Updated Full Name", "PHARMACIST", true);
        mockMvc.perform(put("/api/employees/" + emp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Full Name"))
                .andExpect(jsonPath("$.role").value("PHARMACIST"));

        // Deactivate
        mockMvc.perform(put("/api/employees/" + emp.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Reset password
        ResetPasswordRequestDto resetReq = new ResetPasswordRequestDto("NewSecurePass@999");
        mockMvc.perform(put("/api/employees/" + emp.getId() + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }
}
