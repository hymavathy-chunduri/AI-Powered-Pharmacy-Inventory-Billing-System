package com.pharmacy.pharmacy_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy_backend.dto.BillItemRequestDto;
import com.pharmacy.pharmacy_backend.dto.BillRequestDto;
import com.pharmacy.pharmacy_backend.entity.Customer;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.repository.CustomerRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Test
    void testCreateBillSuccess() throws Exception {
        Customer cust = customerRepository.findAll().stream().findFirst().orElse(null);
        Medicine med = medicineRepository.findAll().stream()
                .filter(m -> m.getStockQuantity() != null && m.getStockQuantity() >= 2)
                .findFirst().orElse(null);

        if (cust != null && med != null) {
            BillRequestDto req = new BillRequestDto();
            req.setCustomerId(cust.getCustomerId());
            req.setPaymentMode("CASH");

            BillItemRequestDto item = new BillItemRequestDto();
            item.setMedicineId(med.getMedicineId());
            item.setQuantity(2);
            req.setItems(List.of(item));

            mockMvc.perform(post("/api/bills")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.billId").exists())
                    .andExpect(jsonPath("$.totalAmount").exists());
        }
    }

    @Test
    void testInsufficientStockRejection() throws Exception {
        Customer cust = customerRepository.findAll().stream().findFirst().orElse(null);
        Medicine med = medicineRepository.findAll().stream().findFirst().orElse(null);

        if (cust != null && med != null) {
            BillRequestDto req = new BillRequestDto();
            req.setCustomerId(cust.getCustomerId());
            req.setPaymentMode("UPI");

            BillItemRequestDto item = new BillItemRequestDto();
            item.setMedicineId(med.getMedicineId());
            item.setQuantity(999999); // Exceeds available stock
            req.setItems(List.of(item));

            mockMvc.perform(post("/api/bills")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
        }
    }
}
