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
 * Uses embedded Flapdoodle MongoDB (no PostgreSQL required).
 * @Transactional removed — MongoDB doesn't use JPA transactions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"ADMIN"})
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCategoryCRUD() throws Exception {
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

        // getCategoryId() returns the String MongoDB ObjectId
        mockMvc.perform(get("/api/categories/" + created.getCategoryId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value(cat.getCategoryName()));
    }
}
