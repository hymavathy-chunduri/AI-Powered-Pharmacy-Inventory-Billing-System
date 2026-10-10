package com.pharmacy.pharmacy_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Audit document stored in the separate 'pharmacy_audit_db' database in collection 'login_history'.
 * Tracks authentication events without storing sensitive credentials, hashes, or tokens.
 */
@Document(collection = "login_history")
@CompoundIndex(name = "emp_time_idx", def = "{'employee_id': 1, 'timestamp': -1}")
public class LoginHistory {

    @Id
    private String id;

    @Indexed
    @Field("employee_id")
    private String employeeId;

    @Field("employee_name")
    private String employeeName;

    @Indexed
    @Field("event_type")
    private String eventType; // LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, SESSION_EXPIRED

    @Indexed
    @Field("timestamp")
    private Instant timestamp; // UTC timestamp

    @Field("status")
    private String status; // SUCCESS, FAILURE

    @Field("failure_reason")
    private String failureReason; // Safe category: INVALID_CREDENTIALS, INACTIVE_ACCOUNT, ACCOUNT_LOCKED, etc.

    @JsonIgnore
    @Field("session_reference")
    private String sessionReference; // Safe non-reversible session identifier hash (hidden from API responses)

    @Field("ip_address")
    private String ipAddress;

    @Field("user_agent")
    private String userAgent;

    public LoginHistory() {
        this.timestamp = Instant.now();
    }

    public LoginHistory(String employeeId, String employeeName, String eventType, String status, String failureReason, String sessionReference, String ipAddress, String userAgent) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.status = status;
        this.failureReason = failureReason;
        this.sessionReference = sessionReference;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getSessionReference() {
        return sessionReference;
    }

    public void setSessionReference(String sessionReference) {
        this.sessionReference = sessionReference;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
