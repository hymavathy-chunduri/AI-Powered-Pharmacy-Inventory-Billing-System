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

    public Medicine getMedicineById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + id));
    }

    public List<Medicine> searchMedicines(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllMedicines();
        }
        return medicineRepository.findByMedicineNameContainingIgnoreCase(query.trim());
    }

    public Medicine createMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    public Medicine updateMedicine(Long id, Medicine details) {
        Medicine existing = getMedicineById(id);
        existing.setMedicineName(details.getMedicineName());
        existing.setCategory(details.getCategory());
        if (details.getPrice() != null) existing.setPrice(details.getPrice());
        if (details.getStockQuantity() != null) existing.setStockQuantity(details.getStockQuantity());
        if (details.getManufactureDate() != null) existing.setManufactureDate(details.getManufactureDate());
        if (details.getExpiryDate() != null) existing.setExpiryDate(details.getExpiryDate());
        return medicineRepository.save(existing);
    }

    public void deleteMedicine(Long id) {
        Medicine existing = getMedicineById(id);
        medicineRepository.delete(existing);
    }

    public List<Medicine> getLowStockMedicines(Integer threshold) {
        int limit = (threshold != null) ? threshold : 15;
        try {
            List<Medicine> viewResult = medicineRepository.findLowStockFromView();
            if (!viewResult.isEmpty()) return viewResult;
        } catch (Exception ignored) {}
        return medicineRepository.findByStockQuantityLessThanEqual(limit);
    }

    public List<Medicine> getExpiryAlerts() {
        try {
            List<Medicine> viewResult = medicineRepository.findExpiryAlertsFromView();
            if (!viewResult.isEmpty()) return viewResult;
        } catch (Exception ignored) {}
        LocalDate cutoff = LocalDate.now().plusDays(30);
        return medicineRepository.findByExpiryDateBefore(cutoff);
    }
}
