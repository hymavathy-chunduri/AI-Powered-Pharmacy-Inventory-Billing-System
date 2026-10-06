package com.pharmacy.pharmacy_backend.repository;

import com.pharmacy.pharmacy_backend.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByMedicineNameContainingIgnoreCase(String name);

    List<Medicine> findByStockQuantityLessThanEqual(Integer threshold);

    List<Medicine> findByExpiryDateBefore(LocalDate date);

    @Query(value = "SELECT * FROM low_stock_view", nativeQuery = true)
    List<Medicine> findLowStockFromView();

    @Query(value = "SELECT * FROM expiry_alert_view", nativeQuery = true)
    List<Medicine> findExpiryAlertsFromView();
}
