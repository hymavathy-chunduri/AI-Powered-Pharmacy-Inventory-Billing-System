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
        org.bson.Document sortStage = new org.bson.Document("$sort",
                new org.bson.Document("bill_date", -1));
        org.bson.Document limitStage = new org.bson.Document("$limit", 50);
        org.bson.Document lookupStage = new org.bson.Document("$lookup", new org.bson.Document()
                .append("from", "customers")
                .append("localField", "customer")
                .append("foreignField", "_id")
                .append("as", "customer_doc")
        );

        List<org.bson.Document> pipeline = Arrays.asList(sortStage, limitStage, lookupStage);

        List<org.bson.Document> raw = new ArrayList<>();
        mongoTemplate.getDb().getCollection("bills").aggregate(pipeline).into(raw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (org.bson.Document doc : raw) {
            Map<String, Object> row = new LinkedHashMap<>();
            Object idObj = doc.get("_id");
            String idStr = idObj != null ? idObj.toString() : "";
            row.put("bill_id", idStr);
            row.put("billId", idStr);

            @SuppressWarnings("unchecked")
            List<org.bson.Document> custDocs = (List<org.bson.Document>) doc.get("customer_doc");
            String custName = "Customer";
            if (custDocs != null && !custDocs.isEmpty()) {
                custName = custDocs.get(0).getString("customer_name");
                if (custName == null) custName = "Customer";
            }
            row.put("customer_name", custName);
            row.put("customerName", custName);

            Object bDate = doc.get("bill_date");
            if (bDate instanceof java.util.Date) {
                row.put("bill_date", new java.text.SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) bDate));
            } else if (bDate != null) {
                row.put("bill_date", bDate.toString());
            } else {
                row.put("bill_date", "Recent");
            }

            Object total = doc.get("total_amount");
            if (total instanceof org.bson.types.Decimal128) {
                row.put("total_amount", ((org.bson.types.Decimal128) total).bigDecimalValue());
            } else if (total instanceof Double) {
                row.put("total_amount", BigDecimal.valueOf((Double) total));
            } else if (total instanceof Number) {
                row.put("total_amount", BigDecimal.valueOf(((Number) total).doubleValue()));
            } else {
                row.put("total_amount", BigDecimal.ZERO);
            }
            row.put("totalAmount", row.get("total_amount"));
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
