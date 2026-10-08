package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.entity.StockAudit;
import com.pharmacy.pharmacy_backend.repository.BillRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import com.pharmacy.pharmacy_backend.repository.StockAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

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
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ReportService(MedicineRepository medicineRepository,
                         BillRepository billRepository,
                         StockAuditRepository stockAuditRepository,
                         MongoTemplate mongoTemplate) {
        this.medicineRepository = medicineRepository;
        this.billRepository = billRepository;
        this.stockAuditRepository = stockAuditRepository;
        this.mongoTemplate = mongoTemplate;
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
     * Uses MongoDB server-side aggregation ($unwind + $group) — avoids loading all bills into memory
     * and eliminates N+1 customer DBRef resolution overhead on Atlas M0.
     */
    public List<Map<String, Object>> getSalesReport() {
        org.bson.Document unwindStage = new org.bson.Document("$unwind", "$items");
        org.bson.Document groupStage = new org.bson.Document("$group", new org.bson.Document()
                .append("_id", "$items.medicine_id")
                .append("medicine_name", new org.bson.Document("$first", "$items.medicine_name"))
                .append("total_quantity_sold", new org.bson.Document("$sum", "$items.quantity"))
                .append("total_revenue", new org.bson.Document("$sum", "$items.subtotal"))
        );
        org.bson.Document sortStage = new org.bson.Document("$sort",
                new org.bson.Document("total_revenue", -1));

        List<org.bson.Document> pipeline = Arrays.asList(unwindStage, groupStage, sortStage);

        List<org.bson.Document> raw = new ArrayList<>();
        mongoTemplate.getDb().getCollection("bills").aggregate(pipeline).into(raw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (org.bson.Document doc : raw) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("medicine_id", doc.getString("_id"));
            row.put("medicine_name", doc.getString("medicine_name"));
            row.put("total_quantity_sold", doc.getInteger("total_quantity_sold", 0));
            // Handle both Double and Decimal128 from MongoDB
            Object rev = doc.get("total_revenue");
            if (rev instanceof org.bson.types.Decimal128) {
                row.put("total_revenue", ((org.bson.types.Decimal128) rev).bigDecimalValue());
            } else if (rev instanceof Double) {
                row.put("total_revenue", BigDecimal.valueOf((Double) rev));
            } else {
                row.put("total_revenue", BigDecimal.ZERO);
            }
            result.add(row);
        }
        return result;
    }

    /**
     * Returns all stock audit logs, newest first.
     */
    public List<StockAudit> getStockAuditLogs() {
        return stockAuditRepository.findAllByOrderByChangedAtDesc();
    }
}
