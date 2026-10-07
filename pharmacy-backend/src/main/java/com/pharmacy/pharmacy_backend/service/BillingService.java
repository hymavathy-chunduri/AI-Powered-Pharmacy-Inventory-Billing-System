package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.dto.BillItemRequestDto;
import com.pharmacy.pharmacy_backend.dto.BillRequestDto;
import com.pharmacy.pharmacy_backend.entity.Bill;
import com.pharmacy.pharmacy_backend.entity.BillItem;
import com.pharmacy.pharmacy_backend.entity.Customer;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.entity.StockAudit;
import com.pharmacy.pharmacy_backend.exception.InsufficientStockException;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.BillRepository;
import com.pharmacy.pharmacy_backend.repository.CustomerRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import com.pharmacy.pharmacy_backend.repository.StockAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BillingService — replaces the PostgreSQL triggers:
 *   trg_reduce_stock    → reduce_stock()
 *   trg_stock_audit     → audit_stock_change()
 *   trg_update_bill_total → update_bill_total()
 *
 * Business flow:
 *   1. Validate customer exists
 *   2. Pre-validate ALL items for sufficient stock → reject HTTP 400 if any fail
 *   3. Build Bill with embedded BillItems
 *   4. Decrease medicine stock (was PostgreSQL trigger)
 *   5. Write StockAudit entry for each item (was PostgreSQL trigger)
 *   6. Calculate total (was PostgreSQL trigger)
 *   7. Save bill
 */
@Service
public class BillingService {

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final MedicineRepository medicineRepository;
    private final StockAuditRepository stockAuditRepository;

    @Autowired
    public BillingService(BillRepository billRepository,
                          CustomerRepository customerRepository,
                          MedicineRepository medicineRepository,
                          StockAuditRepository stockAuditRepository) {
        this.billRepository = billRepository;
        this.customerRepository = customerRepository;
        this.medicineRepository = medicineRepository;
        this.stockAuditRepository = stockAuditRepository;
    }

    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    public Bill getBillById(String id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));
    }

    public Bill createBill(BillRequestDto request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));

        // --- PHASE 1: Pre-validate ALL stock levels before making any changes ---
        // This ensures atomicity: if any item fails, no stock is modified.
        for (BillItemRequestDto itemDto : request.getItems()) {
            Medicine medicine = medicineRepository.findById(itemDto.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + itemDto.getMedicineId()));

            int available = medicine.getStockQuantity() != null ? medicine.getStockQuantity() : 0;
            if (available < itemDto.getQuantity()) {
                throw new InsufficientStockException(
                    "Insufficient stock for '" + medicine.getMedicineName() +
                    "'. Available: " + available + ", Requested: " + itemDto.getQuantity()
                );
            }
        }

        // --- PHASE 2: All items validated — now process the bill ---
        Bill bill = new Bill();
        bill.setCustomer(customer);
        bill.setBillDate(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (BillItemRequestDto itemDto : request.getItems()) {
            Medicine medicine = medicineRepository.findById(itemDto.getMedicineId()).get();

            // Build embedded bill item (denormalize medicine name for billing history integrity)
            BillItem item = new BillItem();
            item.setMedicineId(medicine.getId());
            item.setMedicineName(medicine.getMedicineName());
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(medicine.getPrice());
            BigDecimal subtotal = medicine.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            item.setSubtotal(subtotal);
            totalAmount = totalAmount.add(subtotal);
            bill.addItem(item);

            // --- REPLACES trg_reduce_stock / reduce_stock() ---
            int oldStock = medicine.getStockQuantity();
            int newStock = oldStock - itemDto.getQuantity();
            medicine.setStockQuantity(newStock);
            medicineRepository.save(medicine);

            // --- REPLACES trg_stock_audit / audit_stock_change() ---
            stockAuditRepository.save(new StockAudit(medicine, oldStock, newStock));
        }

        // --- REPLACES trg_update_bill_total / update_bill_total() ---
        bill.setTotalAmount(totalAmount);
        return billRepository.save(bill);
    }
}
