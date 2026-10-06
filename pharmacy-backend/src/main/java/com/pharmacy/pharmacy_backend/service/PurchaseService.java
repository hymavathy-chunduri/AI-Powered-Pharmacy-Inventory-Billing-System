package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.dto.PurchaseItemRequestDto;
import com.pharmacy.pharmacy_backend.dto.PurchaseRequestDto;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.entity.Purchase;
import com.pharmacy.pharmacy_backend.entity.PurchaseItem;
import com.pharmacy.pharmacy_backend.entity.Supplier;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import com.pharmacy.pharmacy_backend.repository.PurchaseRepository;
import com.pharmacy.pharmacy_backend.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;

    @Autowired
    public PurchaseService(PurchaseRepository purchaseRepository,
                           SupplierRepository supplierRepository,
                           MedicineRepository medicineRepository) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.medicineRepository = medicineRepository;
    }

    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    public Purchase getPurchaseById(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with ID: " + id));
    }

    @Transactional
    public Purchase createPurchase(PurchaseRequestDto request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + request.getSupplierId()));

        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setPurchaseDate(LocalDate.now());

        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseItemRequestDto itemDto : request.getItems()) {
            Medicine medicine = medicineRepository.findById(itemDto.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + itemDto.getMedicineId()));

            PurchaseItem item = new PurchaseItem();
            item.setMedicine(medicine);
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());

            BigDecimal subtotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            total = total.add(subtotal);
            purchase.addItem(item);
        }

        purchase.setTotalAmount(total);
        return purchaseRepository.save(purchase);
    }
}
