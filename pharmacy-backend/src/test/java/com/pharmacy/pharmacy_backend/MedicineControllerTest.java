package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.entity.Category;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.repository.CategoryRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MedicineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testGetAllMedicines() throws Exception {
        mockMvc.perform(get("/api/medicines"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testSearchMedicine() throws Exception {
        mockMvc.perform(get("/api/medicines?search=Paracetamol"))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateAndUpdateMedicine() throws Exception {
        Category cat = categoryRepository.findAll().stream().findFirst().orElse(null);

        Medicine med = new Medicine();
        med.setMedicineName("Test Medicine JUnit");
        med.setPrice(new BigDecimal("99.50"));
        med.setStockQuantity(100);
        med.setCategory(cat);

        String json = objectMapper.writeValueAsString(med);

        // Create
        String response = mockMvc.perform(post("/api/medicines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicineName").value("Test Medicine JUnit"))
                .andReturn().getResponse().getContentAsString();

        Medicine created = objectMapper.readValue(response, Medicine.class);

        // Update
        created.setMedicineName("Test Medicine Updated");
        mockMvc.perform(put("/api/medicines/" + created.getMedicineId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicineName").value("Test Medicine Updated"));
    }

    @Test
    void testLowStockAndExpiryAlerts() throws Exception {
        mockMvc.perform(get("/api/medicines/low-stock"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/medicines/expiry-alerts"))
                .andExpect(status().isOk());
    }
}
