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

    @GetMapping
    public ResponseEntity<?> getPredictions() {
        try {
            Map<?, ?> response = restTemplate.getForObject(mlServiceUrl, Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "ML_SERVICE_UNAVAILABLE",
                    "message", "Python ML service is currently offline or starting up on port 5001"
            ));
        }
    }

    @GetMapping("/{medicineId}")
    public ResponseEntity<?> getPredictionByMedicineId(@PathVariable Long medicineId) {
        try {
            Map<?, ?> response = restTemplate.getForObject(mlServiceUrl + "/" + medicineId, Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "ML_SERVICE_UNAVAILABLE",
                    "message", "Python ML service is currently offline or starting up on port 5001"
            ));
        }
    }
}
