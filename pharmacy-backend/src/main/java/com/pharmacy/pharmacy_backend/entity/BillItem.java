package com.pharmacy.pharmacy_backend.entity;

import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

/**
 * BillItem is embedded inside Bill document.
 * No @Document annotation — not a standalone collection.
 */
public class BillItem {

    @Field("medicine_id")
    private String medicineId;

    @Field("medicine_name")
    private String medicineName;

    @Field("quantity")
    private Integer quantity;

    @Field("unit_price")
    private BigDecimal unitPrice;

    @Field("subtotal")
    private BigDecimal subtotal;

    public BillItem() {}

    public String getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(String medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
