package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.dto.PurchaseItemRequestDto;
import com.pharmacy.pharmacy_backend.dto.PurchaseRequestDto;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.entity.Purchase;
import com.pharmacy.pharmacy_backend.entity.PurchaseItem;
import com.pharmacy.pharmacy_backend.entity.StockAudit;
import com.pharmacy.pharmacy_backend.entity.Supplier;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import com.pharmacy.pharmacy_backend.repository.PurchaseRepository;
import com.pharmacy.pharmacy_backend.repository.StockAuditRepository;
import com.pharmacy.pharmacy_backend.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PurchaseService — replaces the PostgreSQL triggers:
 *   trg_increase_stock  → increase_stock()
 *   trg_stock_audit     → audit_stock_change()
 *   trg_update_purchase_total → update_purchase_total()
 *
 * Business flow:
 *   1. Validate supplier + medicines exist
 *   2. Build Purchase with embedded PurchaseItems
 *   3. Calculate total
 *   4. Increase medicine stock (was PostgreSQL trigger)
 *   5. Write StockAudit entry (was PostgreSQL trigger)
 *   6. Save purchase
 */
@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final StockAuditRepository stockAuditRepository;

    @Autowired
    public PurchaseService(PurchaseRepository purchaseRepository,
                           SupplierRepository supplierRepository,
                           MedicineRepository medicineRepository,
                           StockAuditRepository stockAuditRepository) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.medicineRepository = medicineRepository;
        this.stockAuditRepository = stockAuditRepository;
    }

    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    public Purchase getPurchaseById(String id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with ID: " + id));
    }

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

            // Build embedded purchase item
            PurchaseItem item = new PurchaseItem();
            item.setMedicineId(medicine.getId());
            item.setMedicineName(medicine.getMedicineName());
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            BigDecimal subtotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            item.setSubtotal(subtotal);
            total = total.add(subtotal);
            purchase.addItem(item);

            // --- REPLACES trg_increase_stock / increase_stock() ---
            int oldStock = medicine.getStockQuantity() != null ? medicine.getStockQuantity() : 0;
            int newStock = oldStock + itemDto.getQuantity();
            medicine.setStockQuantity(newStock);
            medicineRepository.save(medicine);

            // --- REPLACES trg_stock_audit / audit_stock_change() ---
            stockAuditRepository.save(new StockAudit(medicine, oldStock, newStock));
        }

        // --- REPLACES trg_update_purchase_total / update_purchase_total() ---
        purchase.setTotalAmount(total);
        return purchaseRepository.save(purchase);
    }
}
