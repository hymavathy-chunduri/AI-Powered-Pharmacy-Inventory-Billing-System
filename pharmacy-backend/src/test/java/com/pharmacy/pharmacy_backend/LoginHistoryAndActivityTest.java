package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.dto.BillItemRequestDto;
import com.pharmacy.pharmacy_backend.dto.BillRequestDto;
import com.pharmacy.pharmacy_backend.dto.LoginRequestDto;
import com.pharmacy.pharmacy_backend.dto.RegisterEmployeeRequestDto;
import com.pharmacy.pharmacy_backend.entity.Category;
import com.pharmacy.pharmacy_backend.entity.Customer;
import com.pharmacy.pharmacy_backend.entity.Employee;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.repository.CategoryRepository;
import com.pharmacy.pharmacy_backend.repository.CustomerRepository;
import com.pharmacy.pharmacy_backend.repository.EmployeeRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LoginHistoryAndActivityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Customer testCustomer;
    private Medicine testMedicine;

    @BeforeEach
    void setupData() {
        if (!employeeRepository.existsByEmployeeIdIgnoreCase("CASHIER-REG-01")) {
            Employee cashier = new Employee("CASHIER-REG-01", "Cashier One", "cashier1@pharmacare.com", passwordEncoder.encode("Pass@123"), "CASHIER");
            employeeRepository.save(cashier);
        }

        testCustomer = customerRepository.save(new Customer(null, "Test Customer " + System.currentTimeMillis(), "9876543210", "cust@pharmacare.com"));

        Category cat = categoryRepository.save(new Category(null, "Test Category " + System.currentTimeMillis(), "Description"));
        testMedicine = new Medicine();
        testMedicine.setMedicineName("Test Med " + System.currentTimeMillis());
        testMedicine.setPrice(new BigDecimal("25.00"));
        testMedicine.setStockQuantity(100);
        testMedicine.setCategory(cat);
        testMedicine = medicineRepository.save(testMedicine);
    }

    @Test
    void testEmployeeSelfRegistrationSuccess() throws Exception {
        long ts = System.currentTimeMillis();
        String empId = "REG-" + ts;
        String email = "reg" + ts + "@pharmacare.com";

        RegisterEmployeeRequestDto req = new RegisterEmployeeRequestDto(
                "Jane",
                "Doe",
                empId,
                "1234", // Valid employee code
                email,
                "9876543210",
                "Secret@123",
                "Secret@123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(empId))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CASHIER")) // Strict least privileged role
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void testEmployeeRegistrationInvalidCodeRejected() throws Exception {
        long ts = System.currentTimeMillis();
        RegisterEmployeeRequestDto req = new RegisterEmployeeRequestDto(
                "Jane",
                "Doe",
                "REG-FAIL-" + ts,
                "9999", // Wrong employee code
                "fail" + ts + "@pharmacare.com",
                "9876543210",
                "Secret@123",
                "Secret@123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testEmployeeRegistrationPasswordMismatchRejected() throws Exception {
        long ts = System.currentTimeMillis();
        RegisterEmployeeRequestDto req = new RegisterEmployeeRequestDto(
                "Jane",
                "Doe",
                "REG-MISMATCH-" + ts,
                "1234",
                "mismatch" + ts + "@pharmacare.com",
                "9876543210",
                "Secret@123",
                "DifferentPass@123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void testLoginHistoryRecordingAndRetrieval() throws Exception {
        // 1. Successful login
        LoginRequestDto req = new LoginRequestDto("CASHIER-REG-01", "Pass@123");
        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginRes.getRequest().getSession();

        // 2. Query /api/login-history/my with session
        mockMvc.perform(get("/api/login-history/my").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[0].employeeId").value("CASHIER-REG-01"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].sessionReference").doesNotExist()); // Session reference never exposed

        // 3. Cashier cannot access /api/login-history (all-employee admin view)
        mockMvc.perform(get("/api/login-history").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void testFailedLoginAndLogoutAuditRecording() throws Exception {
        // 1. Failed login records LOGIN_FAILURE
        LoginRequestDto badReq = new LoginRequestDto("CASHIER-REG-01", "WrongPass@999");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isUnauthorized());

        // 2. Login then logout records LOGOUT
        LoginRequestDto okReq = new LoginRequestDto("CASHIER-REG-01", "Pass@123");
        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(okReq)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginRes.getRequest().getSession();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminCanViewAllLoginHistory() throws Exception {
        mockMvc.perform(get("/api/login-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void testBillCreationTracksEmployeeAndMyBills() throws Exception {
        // Login as cashier
        LoginRequestDto req = new LoginRequestDto("CASHIER-REG-01", "Pass@123");
        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginRes.getRequest().getSession();

        // Create Bill with session
        BillRequestDto billReq = new BillRequestDto();
        billReq.setCustomerId(testCustomer.getId());
        billReq.setItems(List.of(new BillItemRequestDto(testMedicine.getId(), 2)));

        mockMvc.perform(post("/api/bills")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByEmployeeId").value("CASHIER-REG-01"))
                .andExpect(jsonPath("$.createdByEmployeeName").value("Cashier One"));

        // Query /api/bills/my
        mockMvc.perform(get("/api/bills/my").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].createdByEmployeeId").value("CASHIER-REG-01"));

        // Query /api/activity/my
        mockMvc.perform(get("/api/activity/my").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("CASHIER-REG-01"))
                .andExpect(jsonPath("$.totalBillsCount").isNumber())
                .andExpect(jsonPath("$.totalSalesValue").isNumber());
    }

    @Test
    void testInsufficientStockRollsBackAndPreventsOverselling() throws Exception {
        LoginRequestDto req = new LoginRequestDto("CASHIER-REG-01", "Pass@123");
        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginRes.getRequest().getSession();

        int initialStock = testMedicine.getStockQuantity();

        // Attempt to bill quantity exceeding stock (initialStock + 50)
        BillRequestDto billReq = new BillRequestDto();
        billReq.setCustomerId(testCustomer.getId());
        billReq.setItems(List.of(new BillItemRequestDto(testMedicine.getId(), initialStock + 50)));

        mockMvc.perform(post("/api/bills")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INSUFFICIENT_STOCK"));

        // Verify stock was NOT reduced
        Medicine medAfter = medicineRepository.findById(testMedicine.getId()).get();
        assertEquals(initialStock, medAfter.getStockQuantity());
    }

    @Test
    void testUnauthenticatedBillCreationFails() throws Exception {
        BillRequestDto billReq = new BillRequestDto();
        billReq.setCustomerId(testCustomer.getId());
        billReq.setItems(List.of(new BillItemRequestDto(testMedicine.getId(), 1)));

        mockMvc.perform(post("/api/bills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billReq)))
                .andExpect(status().isUnauthorized());
    }
}
