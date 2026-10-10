package com.pharmacy.pharmacy_backend.controller;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final MongoTemplate mongoTemplate;

    @Autowired(required = false)
    public HealthController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "PharmaCare Backend");
        response.put("timestamp", Instant.now().toString());

        if (mongoTemplate == null) {
            response.put("database", "NOT_CONFIGURED");
            return ResponseEntity.ok(response);
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> future = executor.submit(() -> {
                mongoTemplate.getDb().runCommand(new Document("ping", 1));
                return true;
            });
            boolean dbOk = future.get(3, TimeUnit.SECONDS);
            response.put("database", dbOk ? "UP" : "DOWN");
        } catch (TimeoutException e) {
            response.put("database", "TIMEOUT");
            response.put("databaseError", "MongoDB ping timed out after 3s (cluster may be paused or unreachable)");
        } catch (Exception e) {
            response.put("database", "DOWN");
            response.put("databaseError", "Connection failed: " + (e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName()));
        } finally {
            executor.shutdownNow();
        }

        return ResponseEntity.ok(response);
    }
}
