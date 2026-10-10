package com.pharmacy.pharmacy_backend.service;

import com.mongodb.client.MongoClient;
import com.pharmacy.pharmacy_backend.entity.LoginHistory;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class LoginHistoryService {

    private static final Logger log = LoggerFactory.getLogger(LoginHistoryService.class);

    private final MongoTemplate auditMongoTemplate;

    @Autowired
    public LoginHistoryService(MongoClient mongoClient,
                               MongoConverter mongoConverter,
                               @Value("${mongodb.audit.database:pharmacy_audit_db}") String auditDatabaseName) {
        MongoDatabaseFactory auditFactory = new SimpleMongoClientDatabaseFactory(mongoClient, auditDatabaseName);
        this.auditMongoTemplate = new MongoTemplate(auditFactory, mongoConverter);
    }

    /**
     * Resiliently records a login history / session event in the separate audit database.
     * Guaranteed never to throw exceptions or break business logic if audit writing fails.
     */
    public void recordEvent(String employeeId,
                            String employeeName,
                            String eventType,
                            String status,
                            String failureReason,
                            String sessionId,
                            HttpServletRequest request) {
        try {
            String ipAddress = null;
            String userAgent = null;

            if (request != null) {
                // Use getRemoteAddr() directly to avoid trusting arbitrary client-spoofed headers
                ipAddress = request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 200) {
                    userAgent = userAgent.substring(0, 200);
                }
            }

            String sessionRef = null;
            if (sessionId != null && !sessionId.isBlank()) {
                sessionRef = hashSessionId(sessionId);
            }

            LoginHistory event = new LoginHistory(
                    employeeId != null ? employeeId.trim() : "UNKNOWN",
                    employeeName != null ? employeeName.trim() : null,
                    eventType,
                    status,
                    failureReason,
                    sessionRef,
                    ipAddress,
                    userAgent
            );

            auditMongoTemplate.save(event);
            log.info("Recorded audit event: type={}, employeeId={}, status={}", eventType, employeeId, status);
        } catch (Exception e) {
            log.error("CRITICAL AUDIT ERROR: Failed to record login history event to audit database for employeeId={}: {}", employeeId, e.getMessage(), e);
        }
    }

    public Map<String, Object> getHistory(String employeeId,
                                          String eventType,
                                          String status,
                                          Instant startDate,
                                          Instant endDate,
                                          int page,
                                          int size) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (employeeId != null && !employeeId.isBlank()) {
            criteriaList.add(Criteria.where("employee_id").regex(employeeId.trim(), "i"));
        }
        if (eventType != null && !eventType.isBlank()) {
            criteriaList.add(Criteria.where("event_type").is(eventType.trim()));
        }
        if (status != null && !status.isBlank()) {
            criteriaList.add(Criteria.where("status").is(status.trim().toUpperCase()));
        }
        if (startDate != null || endDate != null) {
            Criteria timeCriteria = Criteria.where("timestamp");
            if (startDate != null) timeCriteria = timeCriteria.gte(startDate);
            if (endDate != null) timeCriteria = timeCriteria.lte(endDate);
            criteriaList.add(timeCriteria);
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long totalElements = auditMongoTemplate.count(query, LoginHistory.class);

        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        query.skip((long) Math.max(0, page) * size);
        query.limit(Math.max(1, size));

        List<LoginHistory> content = auditMongoTemplate.find(query, LoginHistory.class);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("pageSize", size);
        return result;
    }

    public Map<String, Object> getEmployeeHistory(String employeeId, int page, int size) {
        Query query = new Query(Criteria.where("employee_id").is(employeeId));
        long totalElements = auditMongoTemplate.count(query, LoginHistory.class);

        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        query.skip((long) Math.max(0, page) * size);
        query.limit(Math.max(1, size));

        List<LoginHistory> content = auditMongoTemplate.find(query, LoginHistory.class);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("pageSize", size);
        return result;
    }

    public List<LoginHistory> getRecentEventsForEmployee(String employeeId, int limit) {
        Query query = new Query(Criteria.where("employee_id").is(employeeId))
                .with(Sort.by(Sort.Direction.DESC, "timestamp"))
                .limit(limit);
        return auditMongoTemplate.find(query, LoginHistory.class);
    }

    private String hashSessionId(String sessionId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sessionId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(8, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "anon-sess";
        }
    }
}
