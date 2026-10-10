package com.pharmacy.pharmacy_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"ADMIN"})
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testInventoryReport() throws Exception {
        mockMvc.perform(get("/api/reports/inventory"))
                .andExpect(status().isOk());
    }

    @Test
    void testSalesReport() throws Exception {
        mockMvc.perform(get("/api/reports/sales"))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditLogsReport() throws Exception {
        mockMvc.perform(get("/api/reports/audit"))
                .andExpect(status().isOk());
    }
}
