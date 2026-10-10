package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.dto.BillItemRequestDto;
import com.pharmacy.pharmacy_backend.dto.BillRequestDto;
import com.pharmacy.pharmacy_backend.entity.Bill;
import com.pharmacy.pharmacy_backend.entity.BillItem;
import com.pharmacy.pharmacy_backend.entity.Customer;
import com.pharmacy.pharmacy_backend.entity.Employee;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.entity.StockAudit;
import com.pharmacy.pharmacy_backend.exception.InsufficientStockException;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.BillRepository;
import com.pharmacy.pharmacy_backend.repository.CustomerRepository;
import com.pharmacy.pharmacy_backend.repository.EmployeeRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import com.pharmacy.pharmacy_backend.repository.StockAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final MedicineRepository medicineRepository;
    private final StockAuditRepository stockAuditRepository;
    private final EmployeeRepository employeeRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public BillingService(BillRepository billRepository,
                          CustomerRepository customerRepository,
                          MedicineRepository medicineRepository,
                          StockAuditRepository stockAuditRepository,
                          EmployeeRepository employeeRepository,
                          MongoTemplate mongoTemplate) {
        this.billRepository = billRepository;
        this.customerRepository = customerRepository;
        this.medicineRepository = medicineRepository;
        this.stockAuditRepository = stockAuditRepository;
        this.employeeRepository = employeeRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Returns recent bills. Role-aware:
     * - Admins can view all bills or filter by any employee ID.
     * - Non-admins (Pharmacists, Cashiers) can only view their own bills.
     */
    public List<Bill> getAllBills(String employeeIdFilter) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            String currentEmployeeId = auth != null ? auth.getName() : "ANONYMOUS";
            return billRepository.findByCreatedByEmployeeIdOrderByBillDateDesc(currentEmployeeId, PageRequest.of(0, 50));
        }

        if (employeeIdFilter != null && !employeeIdFilter.isBlank()) {
            return billRepository.findByCreatedByEmployeeIdOrderByBillDateDesc(employeeIdFilter.trim(), PageRequest.of(0, 50));
        }
        return billRepository.findAllByOrderByBillDateDesc(PageRequest.of(0, 50));
    }

    public List<Bill> getAllBills() {
        return getAllBills(null);
    }

    public List<Bill> getMyBills(String employeeId) {
        return billRepository.findByCreatedByEmployeeIdOrderByBillDateDesc(employeeId, PageRequest.of(0, 100));
    }

    public Bill getBillById(String id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && auth != null && !auth.getName().equalsIgnoreCase(bill.getCreatedByEmployeeId())) {
            throw new AccessDeniedException("Access denied: You are not authorized to view another employee's bill.");
        }

        return bill;
    }

    /**
     * Creates a bill with atomic concurrency control and compensating rollback:
     * 1. Requires authenticated employee.
     * 2. Validates customer and items (min 1 item, positive quantities).
     * 3. Aggregates duplicate medicine entries to prevent overselling.
     * 4. Pre-validates stock levels.
     * 5. Atomically decrements stock using conditional findAndModify (stock_quantity >= quantity).
     * 6. If any step fails, rolls back all decremented medicines safely.
     * 7. Records audit logs and persists the bill.
     */
    public Bill createBill(BillRequestDto request) {
        // Enforce authenticated employee identity
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required: Unauthenticated requests cannot create bills.");
        }
        String currentEmployeeId = auth.getName();

        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw new IllegalArgumentException("Customer ID is required.");
        }
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Bill must contain at least one item.");
        }

        // --- STEP 1: Aggregate duplicate medicine quantities & validate positive quantities ---
        Map<String, Integer> aggregatedQuantities = new LinkedHashMap<>();
        for (BillItemRequestDto itemDto : request.getItems()) {
            if (itemDto.getMedicineId() == null || itemDto.getMedicineId().isBlank()) {
                throw new IllegalArgumentException("Medicine ID is required for each bill item.");
            }
            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be a positive number.");
            }
            aggregatedQuantities.merge(itemDto.getMedicineId().trim(), itemDto.getQuantity(), Integer::sum);
        }

        // --- STEP 2: Pre-validate that all medicines exist and have sufficient stock ---
        Map<String, Medicine> preloadedMedicines = new HashMap<>();
        for (Map.Entry<String, Integer> entry : aggregatedQuantities.entrySet()) {
            String medId = entry.getKey();
            int requiredQty = entry.getValue();

            Medicine medicine = medicineRepository.findById(medId)
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + medId));

            int available = medicine.getStockQuantity() != null ? medicine.getStockQuantity() : 0;
            if (available < requiredQty) {
                throw new InsufficientStockException(
                        "Insufficient stock for '" + medicine.getMedicineName() +
                        "'. Available: " + available + ", Requested: " + requiredQty
                );
            }
            preloadedMedicines.put(medId, medicine);
        }

        // --- STEP 3: Atomic conditional stock deduction with compensating rollback ---
        Map<String, Integer> successfullyDeducted = new HashMap<>();
        Map<String, Integer> originalStockLevels = new HashMap<>();
        Map<String, Integer> finalStockLevels = new HashMap<>();

        try {
            for (Map.Entry<String, Integer> entry : aggregatedQuantities.entrySet()) {
                String medId = entry.getKey();
                int qtyToDeduct = entry.getValue();

                Query conditionalQuery = new Query(
                        Criteria.where("_id").is(medId).and("stock_quantity").gte(qtyToDeduct)
                );
                Update decrementUpdate = new Update().inc("stock_quantity", -qtyToDeduct);

                Medicine updatedMed = mongoTemplate.findAndModify(
                        conditionalQuery,
                        decrementUpdate,
                        FindAndModifyOptions.options().returnNew(true),
                        Medicine.class
                );

                if (updatedMed == null) {
                    // Another concurrent transaction reduced stock in the meantime!
                    Medicine currentDbMed = medicineRepository.findById(medId).orElse(null);
                    int currentAvail = (currentDbMed != null && currentDbMed.getStockQuantity() != null)
                            ? currentDbMed.getStockQuantity() : 0;
                    Medicine ref = preloadedMedicines.get(medId);
                    String medName = ref != null ? ref.getMedicineName() : medId;

                    throw new InsufficientStockException(
                            "Concurrent stock conflict: Insufficient stock for '" + medName +
                            "'. Available: " + currentAvail + ", Requested: " + qtyToDeduct
                    );
                }

                int newStock = updatedMed.getStockQuantity();
                int oldStock = newStock + qtyToDeduct;

                successfullyDeducted.put(medId, qtyToDeduct);
                originalStockLevels.put(medId, oldStock);
                finalStockLevels.put(medId, newStock);
            }

            // --- STEP 4: Build Bill and embedded items ---
            Bill bill = new Bill();
            bill.setCustomer(customer);
            bill.setBillDate(LocalDateTime.now());
            bill.setCreatedByEmployeeId(currentEmployeeId);

            Optional<Employee> empOpt = employeeRepository.findByEmployeeIdIgnoreCase(currentEmployeeId);
            if (empOpt.isPresent()) {
                bill.setCreatedByEmployeeName(empOpt.get().getFullName());
            } else {
                bill.setCreatedByEmployeeName(currentEmployeeId);
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Map.Entry<String, Integer> entry : aggregatedQuantities.entrySet()) {
                Medicine med = preloadedMedicines.get(entry.getKey());
                int qty = entry.getValue();

                BillItem item = new BillItem();
                item.setMedicineId(med.getId());
                item.setMedicineName(med.getMedicineName());
                item.setQuantity(qty);
                item.setUnitPrice(med.getPrice());
                BigDecimal subtotal = med.getPrice().multiply(BigDecimal.valueOf(qty));
                item.setSubtotal(subtotal);
                totalAmount = totalAmount.add(subtotal);
                bill.addItem(item);

                // Record stock audit
                int oldStock = originalStockLevels.get(med.getId());
                int newStock = finalStockLevels.get(med.getId());
                stockAuditRepository.save(new StockAudit(med, oldStock, newStock));
            }

            bill.setTotalAmount(totalAmount);
            Bill savedBill = billRepository.save(bill);
            log.info("Bill created successfully: id={}, employeeId={}, total={}", savedBill.getId(), currentEmployeeId, totalAmount);
            return savedBill;

        } catch (Exception ex) {
            // Compensating rollback for any medicines decremented prior to failure
            log.warn("Rolling back partially decremented medicines due to bill error: {}", ex.getMessage());
            for (Map.Entry<String, Integer> rollEntry : successfullyDeducted.entrySet()) {
                try {
                    Query rollQuery = new Query(Criteria.where("_id").is(rollEntry.getKey()));
                    Update rollUpdate = new Update().inc("stock_quantity", rollEntry.getValue());
                    mongoTemplate.updateFirst(rollQuery, rollUpdate, Medicine.class);
                } catch (Exception rollEx) {
                    log.error("CRITICAL: Failed to compensate stock for medicine {}: {}", rollEntry.getKey(), rollEx.getMessage());
                }
            }
            if (ex instanceof InsufficientStockException) {
                throw (InsufficientStockException) ex;
            } else if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException("Bill processing failed: " + ex.getMessage(), ex);
            }
        }
    }
}
