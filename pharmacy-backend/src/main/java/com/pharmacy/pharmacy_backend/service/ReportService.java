package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.entity.Bill;
import com.pharmacy.pharmacy_backend.entity.BillItem;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.entity.StockAudit;
import com.pharmacy.pharmacy_backend.repository.BillRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import com.pharmacy.pharmacy_backend.repository.StockAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportService — replaces the following PostgreSQL views:
 *   inventory_view    → getInventoryReport()
 *   sales_report      → getSalesReport()
 *   low_stock_view    → handled in MedicineService.getLowStockMedicines()
 *   expiry_alert_view → handled in MedicineService.getExpiryAlerts()
 */
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

    /**
     * Replaces the PostgreSQL inventory_view.
     * Returns total medicine count, low-stock count, total inventory value, and full medicine list.
     */
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

    /**
     * Replaces the PostgreSQL sales_report view.
     * Builds aggregated sales data from bill documents (embedded bill items).
     */
    public List<Map<String, Object>> getSalesReport() {
        List<Bill> bills = billRepository.findAll();

        // Aggregate sales by medicine across all bills
        Map<String, Map<String, Object>> salesByMedicine = new LinkedHashMap<>();

        for (Bill bill : bills) {
            if (bill.getItems() == null) continue;
            for (BillItem item : bill.getItems()) {
                String medId = item.getMedicineId();
                if (medId == null) continue;

                salesByMedicine.computeIfAbsent(medId, k -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("medicine_id", medId);
                    row.put("medicine_name", item.getMedicineName());
                    row.put("total_quantity_sold", 0);
                    row.put("total_revenue", BigDecimal.ZERO);
                    return row;
                });

                Map<String, Object> row = salesByMedicine.get(medId);
                int currentQty = (int) row.get("total_quantity_sold");
                BigDecimal currentRev = (BigDecimal) row.get("total_revenue");
                row.put("total_quantity_sold", currentQty + item.getQuantity());
                row.put("total_revenue", currentRev.add(
                        item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO));
            }
        }

        return new ArrayList<>(salesByMedicine.values());
    }

    /**
     * Returns all stock audit logs, newest first.
     */
    public List<StockAudit> getStockAuditLogs() {
        return stockAuditRepository.findAllByOrderByChangedAtDesc();
    }
}
