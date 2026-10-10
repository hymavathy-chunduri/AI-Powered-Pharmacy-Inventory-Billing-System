package com.pharmacy.pharmacy_backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bill document — contains embedded BillItem array.
 * Customer is a DBRef (independent collection).
 * Tracks the authenticated employee who created the bill.
 */
@Document(collection = "bills")
public class Bill {

    @Id
    private String id;

    @DocumentReference(lazy = false)
    @Field("customer")
    private Customer customer;

    @Field("bill_date")
    private LocalDateTime billDate;

    @Field("total_amount")
    private BigDecimal totalAmount;

    @Field("created_by_employee_id")
    private String createdByEmployeeId;

    @Field("created_by_employee_name")
    private String createdByEmployeeName;

    @Field("items")
    private List<BillItem> items = new ArrayList<>();

    public Bill() {}

    public String getBillId() {
        return id;
    }

    public void setBillId(String billId) {
        this.id = billId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDateTime getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDateTime billDate) {
        this.billDate = billDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCreatedByEmployeeId() {
        return createdByEmployeeId;
    }

    public void setCreatedByEmployeeId(String createdByEmployeeId) {
        this.createdByEmployeeId = createdByEmployeeId;
    }

    public String getCreatedByEmployeeName() {
        return createdByEmployeeName;
    }

    public void setCreatedByEmployeeName(String createdByEmployeeName) {
        this.createdByEmployeeName = createdByEmployeeName;
    }

    public List<BillItem> getItems() {
        return items;
    }

    public void setItems(List<BillItem> items) {
        this.items = items;
    }

    public void addItem(BillItem item) {
        items.add(item);
    }
}
