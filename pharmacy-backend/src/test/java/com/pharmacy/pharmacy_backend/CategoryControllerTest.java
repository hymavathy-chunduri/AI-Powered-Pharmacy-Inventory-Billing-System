package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;

/**
 * Validates Category controller operations, role permissions, and regression prevention.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCategoryCRUDForAdmin() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());

        Category cat = new Category();
        cat.setCategoryName("JUnit Category " + System.currentTimeMillis());
        cat.setDescription("Category for testing");

        String response = mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value(cat.getCategoryName()))
                .andReturn().getResponse().getContentAsString();

        Category created = objectMapper.readValue(response, Category.class);

        mockMvc.perform(get("/api/categories/" + created.getCategoryId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value(cat.getCategoryName()));
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    void testCategoryReadPermittedForCashier() throws Exception {
        // Cashiers must be able to view categories in the pharmacy
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    void testCategoryWriteForbiddenForCashier() throws Exception {
        // Cashiers cannot create or modify categories
        Category cat = new Category();
        cat.setCategoryName("Forbidden Category");
        cat.setDescription("Should fail");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isForbidden());
    }
}
