package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.dto.PurchaseItemRequestDto;
import com.pharmacy.pharmacy_backend.dto.PurchaseRequestDto;
import com.pharmacy.pharmacy_backend.entity.Category;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.entity.Supplier;
import com.pharmacy.pharmacy_backend.repository.CategoryRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import com.pharmacy.pharmacy_backend.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testCreatePurchase() throws Exception {
        // Create required test data in embedded MongoDB
        Category cat = categoryRepository.save(new Category(null, "Test Cat " + System.currentTimeMillis(), "desc"));

        Supplier sup = new Supplier();
        sup.setSupplierName("Test Supplier " + System.currentTimeMillis());
        sup = supplierRepository.save(sup);

        Medicine med = new Medicine();
        med.setMedicineName("Test Med " + System.currentTimeMillis());
        med.setPrice(new BigDecimal("20.00"));
        med.setStockQuantity(10);
        med.setCategory(cat);
        med = medicineRepository.save(med);

        PurchaseRequestDto req = new PurchaseRequestDto();
        req.setSupplierId(sup.getSupplierId());

        PurchaseItemRequestDto item = new PurchaseItemRequestDto();
        item.setMedicineId(med.getMedicineId());
        item.setQuantity(50);
        item.setUnitPrice(new BigDecimal("20.00"));
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purchaseId").exists())
                .andExpect(jsonPath("$.totalAmount").value(1000.00));

        // Verify stock was increased (50 + 10 = 60)
        Medicine updated = medicineRepository.findById(med.getMedicineId()).orElseThrow();
        assert updated.getStockQuantity() == 60 : "Stock should be 60 after purchase";
    }
}
