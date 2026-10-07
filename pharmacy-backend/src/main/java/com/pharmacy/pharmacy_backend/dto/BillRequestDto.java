package com.pharmacy.pharmacy_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BillRequestDto {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    private String paymentMode = "CASH";

    @NotEmpty(message = "Bill must contain at least one item")
    @Valid
    private List<BillItemRequestDto> items;

    public BillRequestDto() {}

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
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
