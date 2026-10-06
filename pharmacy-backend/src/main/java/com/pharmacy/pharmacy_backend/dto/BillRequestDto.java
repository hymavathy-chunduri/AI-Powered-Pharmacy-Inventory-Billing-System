package com.pharmacy.pharmacy_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BillRequestDto {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private String paymentMode = "CASH";

    @NotEmpty(message = "Bill must contain at least one item")
    @Valid
    private List<BillItemRequestDto> items;

    public BillRequestDto() {}

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public List<BillItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<BillItemRequestDto> items) {
        this.items = items;
    }
}
