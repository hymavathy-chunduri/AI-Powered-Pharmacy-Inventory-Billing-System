package com.pharmacy.pharmacy_backend.repository;

import com.pharmacy.pharmacy_backend.entity.StockAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAuditRepository extends MongoRepository<StockAudit, String> {

    List<StockAudit> findByMedicineIdOrderByChangedAtDesc(String medicineId);

    List<StockAudit> findAllByOrderByChangedAtDesc();
}
