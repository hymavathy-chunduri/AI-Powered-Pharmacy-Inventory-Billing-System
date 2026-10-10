package com.pharmacy.pharmacy_backend.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "medicines")
public class Medicine {

    @Id
    private String id;

    @NotBlank(message = "Medicine name is required")
    @Field("medicine_name")
    private String medicineName;

    /**
     * Category stored as a DBRef — independent collection, referenced by ID.
     * This preserves the relational semantic: medicines belong to a category.
     */
    @NotNull(message = "Category is required")
    @DocumentReference(lazy = false)
    private Category category;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Field("price")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Field("stock_quantity")
    private Integer stockQuantity;

    @Field("manufacture_date")
    private LocalDate manufactureDate;

    @Field("expiry_date")
    private LocalDate expiryDate;

    public Medicine() {}

    // Alias getters for JSON / frontend compatibility
    public String getMedicineId() {
        return id;
    }

    public void setMedicineId(String medicineId) {
        this.id = medicineId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(LocalDate manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
