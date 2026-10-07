package com.pharmacy.pharmacy_backend.entity;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "customers")
public class Customer {

    @Id
    private String id;

    @NotBlank(message = "Customer name is required")
    @Field("customer_name")
    private String customerName;

    @Field("phone")
    private String phone;

    @Field("email")
    private String email;

    public Customer() {}

    public Customer(String id, String customerName, String phone, String email) {
        this.id = id;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
    }

    public String getCustomerId() {
        return id;
    }

    public void setCustomerId(String customerId) {
        this.id = customerId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
