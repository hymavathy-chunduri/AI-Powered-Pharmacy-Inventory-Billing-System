package com.pharmacy.pharmacy_backend.controller;

import com.pharmacy.pharmacy_backend.dto.PurchaseRequestDto;
import com.pharmacy.pharmacy_backend.entity.Purchase;
import com.pharmacy.pharmacy_backend.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Autowired
    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public ResponseEntity<List<Purchase>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Purchase> getPurchaseById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id));
    }

    @PostMapping
    public ResponseEntity<Purchase> createPurchase(@Valid @RequestBody PurchaseRequestDto request) {
        Purchase created = purchaseService.createPurchase(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
