package com.pharmacy.pharmacy_backend.controller;

import com.pharmacy.pharmacy_backend.dto.BillRequestDto;
import com.pharmacy.pharmacy_backend.entity.Bill;
import com.pharmacy.pharmacy_backend.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin
public class BillingController {

    private final BillingService billingService;

    @Autowired
    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    public ResponseEntity<List<Bill>> getAllBills() {
        return ResponseEntity.ok(billingService.getAllBills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bill> getBillById(@PathVariable String id) {
        return ResponseEntity.ok(billingService.getBillById(id));
    }

    @PostMapping
    public ResponseEntity<Bill> createBill(@Valid @RequestBody BillRequestDto request) {
        Bill created = billingService.createBill(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
