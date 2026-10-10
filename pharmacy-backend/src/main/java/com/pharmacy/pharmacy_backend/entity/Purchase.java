package com.pharmacy.pharmacy_backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase document — contains embedded PurchaseItem array.
 * Supplier is a DBRef (independent collection).
 * Business logic (stock increase + audit) is handled in PurchaseService.
 */
@Document(collection = "purchases")
public class Purchase {

    @Id
    private String id;

    @DocumentReference(lazy = false)
    @Field("supplier")
    private Supplier supplier;

    @Field("purchase_date")
    private LocalDate purchaseDate;

    @Field("total_amount")
    private BigDecimal totalAmount;

    @Field("items")
    private List<PurchaseItem> items = new ArrayList<>();

    public Purchase() {}

    public String getPurchaseId() {
        return id;
    }

    public void setPurchaseId(String purchaseId) {
        this.id = purchaseId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<PurchaseItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseItem> items) {
        this.items = items;
    }

    public void addItem(PurchaseItem item) {
        items.add(item);
    }
}
