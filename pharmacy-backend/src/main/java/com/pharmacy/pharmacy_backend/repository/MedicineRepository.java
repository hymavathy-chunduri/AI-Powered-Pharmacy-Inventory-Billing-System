package com.pharmacy.pharmacy_backend.repository;

import com.pharmacy.pharmacy_backend.entity.Medicine;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicineRepository extends MongoRepository<Medicine, String> {

    List<Medicine> findByMedicineNameContainingIgnoreCase(String name);

    List<Medicine> findByStockQuantityLessThanEqual(Integer threshold);

    List<Medicine> findByExpiryDateBefore(LocalDate date);
}
