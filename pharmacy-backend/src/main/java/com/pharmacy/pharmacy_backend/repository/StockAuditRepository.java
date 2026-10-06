package com.pharmacy.pharmacy_backend.repository;

import com.pharmacy.pharmacy_backend.entity.StockAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAuditRepository extends JpaRepository<StockAudit, Long> {
    List<StockAudit> findByMedicineMedicineIdOrderByChangedAtDesc(Long medicineId);
    List<StockAudit> findAllByOrderByChangedAtDesc();
}
