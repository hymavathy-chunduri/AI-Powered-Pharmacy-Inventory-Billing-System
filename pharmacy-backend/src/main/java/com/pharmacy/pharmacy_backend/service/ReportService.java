package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.entity.StockAudit;
import com.pharmacy.pharmacy_backend.repository.BillRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import com.pharmacy.pharmacy_backend.repository.StockAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final MedicineRepository medicineRepository;
    private final BillRepository billRepository;
    private final StockAuditRepository stockAuditRepository;

    @Autowired
    public ReportService(MedicineRepository medicineRepository,
                         BillRepository billRepository,
                         StockAuditRepository stockAuditRepository) {
        this.medicineRepository = medicineRepository;
        this.billRepository = billRepository;
        this.stockAuditRepository = stockAuditRepository;
    }

    public Map<String, Object> getInventoryReport() {
        List<Medicine> all = medicineRepository.findAll();
        List<Medicine> lowStock = medicineRepository.findByStockQuantityLessThanEqual(15);
        BigDecimal totalValue = all.stream()
                .filter(m -> m.getPrice() != null && m.getStockQuantity() != null)
                .map(m -> m.getPrice().multiply(BigDecimal.valueOf(m.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("totalMedicines", all.size());
        report.put("lowStockCount", lowStock.size());
        report.put("totalInventoryValue", totalValue);
        report.put("medicines", all);
        return report;
    }

    public List<Map<String, Object>> getSalesReport() {
        try {
            return billRepository.getSalesReportNative();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<StockAudit> getStockAuditLogs() {
        return stockAuditRepository.findAllByOrderByChangedAtDesc();
    }
}
