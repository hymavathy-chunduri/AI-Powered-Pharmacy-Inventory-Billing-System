package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCustomerCRUD() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk());

        Customer customer = new Customer();
        customer.setCustomerName("Rahul Kumar JUnit");
        customer.setPhone("9876543210");
        customer.setEmail("rahul" + System.currentTimeMillis() + "@test.com");

        String response = mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Rahul Kumar JUnit"))
                .andReturn().getResponse().getContentAsString();

        Customer created = objectMapper.readValue(response, Customer.class);

        mockMvc.perform(get("/api/customers/" + created.getCustomerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(customer.getEmail()));
    }
}
