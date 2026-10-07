package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;

    @Autowired
    public MedicineService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public Medicine getMedicineById(String id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + id));
    }

    public List<Medicine> searchMedicines(String name) {
        return medicineRepository.findByMedicineNameContainingIgnoreCase(name);
    }

    public Medicine createMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    public Medicine updateMedicine(String id, Medicine details) {
        Medicine existing = getMedicineById(id);
        existing.setMedicineName(details.getMedicineName());
        existing.setCategory(details.getCategory());
        existing.setPrice(details.getPrice());
        existing.setStockQuantity(details.getStockQuantity());
        existing.setManufactureDate(details.getManufactureDate());
        existing.setExpiryDate(details.getExpiryDate());
        return medicineRepository.save(existing);
    }

    public void deleteMedicine(String id) {
        Medicine existing = getMedicineById(id);
        medicineRepository.delete(existing);
    }

    /**
     * Replaces the PostgreSQL low_stock_view.
     * Returns medicines where stock_quantity <= threshold (default 15).
     */
    public List<Medicine> getLowStockMedicines(Integer threshold) {
        int limit = (threshold != null) ? threshold : 15;
        return medicineRepository.findByStockQuantityLessThanEqual(limit);
    }

    /**
     * Replaces the PostgreSQL expiry_alert_view.
     * Returns medicines expiring within the next 30 days.
     */
    public List<Medicine> getExpiryAlerts() {
        LocalDate cutoff = LocalDate.now().plusDays(30);
        return medicineRepository.findByExpiryDateBefore(cutoff);
    }
}
