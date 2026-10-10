package com.pharmacy.pharmacy_backend.dto;

public class UpdateEmployeeRequestDto {

    private String fullName;
    private String role;
    private Boolean active;

    public UpdateEmployeeRequestDto() {}

    public UpdateEmployeeRequestDto(String fullName, String role, Boolean active) {
        this.fullName = fullName;
        this.role = role;
        this.active = active;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
