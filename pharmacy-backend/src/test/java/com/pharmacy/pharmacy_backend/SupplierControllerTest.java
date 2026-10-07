package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.entity.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSupplierCRUD() throws Exception {
        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk());

        Supplier supplier = new Supplier();
        supplier.setSupplierName("JUnit Supplier " + System.currentTimeMillis());
        supplier.setPhone("9998887776");
        supplier.setEmail("supplier" + System.currentTimeMillis() + "@test.com");
        supplier.setAddress("123 Test Street");

        String response = mockMvc.perform(post("/api/suppliers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(supplier)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierName").value(supplier.getSupplierName()))
                .andReturn().getResponse().getContentAsString();

        Supplier created = objectMapper.readValue(response, Supplier.class);

        mockMvc.perform(get("/api/suppliers/" + created.getSupplierId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("9998887776"));
    }
}
