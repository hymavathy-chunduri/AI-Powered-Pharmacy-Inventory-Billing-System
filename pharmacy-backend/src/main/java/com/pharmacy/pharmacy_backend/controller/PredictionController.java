package com.pharmacy.pharmacy_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
@CrossOrigin
public class PredictionController {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @org.springframework.beans.factory.annotation.Value("${ml.service.url:http://localhost:5001/api/predictions}")
    private String mlServiceUrl;

    /**
     * Resolves and normalizes the base predictions endpoint URL.
     * Handles base host URLs (e.g., http://pharmacy-ml:5001), full endpoint paths
     * (e.g., http://pharmacy-ml:5001/api/predictions), and trailing slashes safely without duplication.
     */
    private String getBaseEndpointUrl() {
        if (mlServiceUrl == null || mlServiceUrl.isBlank()) {
            return "http://localhost:5001/api/predictions";
        }
        String clean = mlServiceUrl.trim();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        if (!clean.endsWith("/api/predictions")) {
            clean = clean + "/api/predictions";
        }
        return clean;
    }

    @GetMapping
    public ResponseEntity<?> getPredictions() {
        try {
            String targetUrl = getBaseEndpointUrl();
            Map<?, ?> response = restTemplate.getForObject(targetUrl, Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "ML_SERVICE_UNAVAILABLE",
                    "message", "Python ML service is currently offline or starting up on port 5001"
            ));
        }
    }

    @GetMapping("/{medicineId}")
    public ResponseEntity<?> getPredictionByMedicineId(@PathVariable String medicineId) {
        try {
            String targetUrl = getBaseEndpointUrl() + "/" + (medicineId != null ? medicineId.trim() : "");
            Map<?, ?> response = restTemplate.getForObject(targetUrl, Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "ML_SERVICE_UNAVAILABLE",
                    "message", "Python ML service is currently offline or starting up on port 5001"
            ));
        }
    }
}
