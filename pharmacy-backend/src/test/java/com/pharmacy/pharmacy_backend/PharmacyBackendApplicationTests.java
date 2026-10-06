package com.pharmacy.pharmacy_backend;

import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class PharmacyBackendApplicationTests {

    @Autowired
    private MedicineRepository medicineRepository;

    @Test
    void contextLoads() {
        assertNotNull(medicineRepository);
    }
}
