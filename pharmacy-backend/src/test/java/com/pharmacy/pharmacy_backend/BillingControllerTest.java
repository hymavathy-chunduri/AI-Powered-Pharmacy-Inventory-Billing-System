package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.dto.BillItemRequestDto;
import com.pharmacy.pharmacy_backend.dto.BillRequestDto;
import com.pharmacy.pharmacy_backend.entity.Category;
import com.pharmacy.pharmacy_backend.entity.Customer;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.repository.CategoryRepository;
import com.pharmacy.pharmacy_backend.repository.CustomerRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
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

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"ADMIN"})
public class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testCreateBillSuccess() throws Exception {
        Category cat = categoryRepository.save(new Category(null, "BillTestCat " + System.currentTimeMillis(), "desc"));
        Customer cust = customerRepository.save(new Customer(null, "Bill Test Customer", "1111111111", "billtest@test.com"));

        Medicine med = new Medicine();
        med.setMedicineName("BillMed " + System.currentTimeMillis());
        med.setPrice(new BigDecimal("50.00"));
        med.setStockQuantity(100);
        med.setCategory(cat);
        med = medicineRepository.save(med);

        BillRequestDto req = new BillRequestDto();
        req.setCustomerId(cust.getCustomerId());
        req.setPaymentMode("CASH");

        BillItemRequestDto item = new BillItemRequestDto(med.getMedicineId(), 2);
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/bills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billId").exists())
                .andExpect(jsonPath("$.totalAmount").exists());

        // Verify stock was reduced (100 - 2 = 98)
        Medicine updated = medicineRepository.findById(med.getMedicineId()).orElseThrow();
        assert updated.getStockQuantity() == 98 : "Stock should be 98 after billing 2 units";
    }

    @Test
    void testInsufficientStockRejection() throws Exception {
        Category cat = categoryRepository.save(new Category(null, "StockTestCat " + System.currentTimeMillis(), "desc"));
        Customer cust = customerRepository.save(new Customer(null, "Stock Test Customer", "2222222222", "stocktest@test.com"));

        Medicine med = new Medicine();
        med.setMedicineName("LowStockMed " + System.currentTimeMillis());
        med.setPrice(new BigDecimal("25.00"));
        med.setStockQuantity(5);   // only 5 in stock
        med.setCategory(cat);
        med = medicineRepository.save(med);

        BillRequestDto req = new BillRequestDto();
        req.setCustomerId(cust.getCustomerId());
        req.setPaymentMode("UPI");

        BillItemRequestDto item = new BillItemRequestDto(med.getMedicineId(), 999999); // request more than available
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/bills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }
}
