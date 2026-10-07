package com.pharmacy.pharmacy_backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * StockAudit document — independent collection.
 * Replaces the PostgreSQL audit_stock_change() trigger.
 * Written by PurchaseService and BillingService whenever stock changes.
 */
@Document(collection = "stock_audit")
public class StockAudit {

    @Id
    private String id;

    @DBRef
    @Field("medicine")
    private Medicine medicine;

    @Field("old_stock")
    private Integer oldStock;

    @Field("new_stock")
    private Integer newStock;

    @Field("changed_at")
    private LocalDateTime changedAt;

    public StockAudit() {}

    public StockAudit(Medicine medicine, Integer oldStock, Integer newStock) {
        this.medicine = medicine;
        this.oldStock = oldStock;
        this.newStock = newStock;
        this.changedAt = LocalDateTime.now();
    }

    public String getAuditId() {
        return id;
    }

    public void setAuditId(String auditId) {
        this.id = auditId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }

    public Integer getOldStock() {
        return oldStock;
    }

    public void setOldStock(Integer oldStock) {
        this.oldStock = oldStock;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public void setNewStock(Integer newStock) {
        this.newStock = newStock;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
