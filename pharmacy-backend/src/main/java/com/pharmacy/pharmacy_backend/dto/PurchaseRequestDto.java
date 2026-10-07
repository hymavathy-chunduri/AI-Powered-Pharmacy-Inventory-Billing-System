package com.pharmacy.pharmacy_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class PurchaseRequestDto {

    @NotBlank(message = "Supplier ID is required")
    private String supplierId;

    @NotEmpty(message = "Purchase must contain at least one item")
    @Valid
    private List<PurchaseItemRequestDto> items;

    public PurchaseRequestDto() {}

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public List<PurchaseItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<PurchaseItemRequestDto> items) {
        this.items = items;
    }
}
