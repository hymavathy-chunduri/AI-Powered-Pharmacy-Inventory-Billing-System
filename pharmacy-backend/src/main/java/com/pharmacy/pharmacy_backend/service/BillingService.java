package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.dto.BillItemRequestDto;
import com.pharmacy.pharmacy_backend.dto.BillRequestDto;
import com.pharmacy.pharmacy_backend.entity.Bill;
import com.pharmacy.pharmacy_backend.entity.BillItem;
import com.pharmacy.pharmacy_backend.entity.Customer;
import com.pharmacy.pharmacy_backend.entity.Medicine;
import com.pharmacy.pharmacy_backend.exception.InsufficientStockException;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.BillRepository;
import com.pharmacy.pharmacy_backend.repository.CustomerRepository;
import com.pharmacy.pharmacy_backend.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillingService {

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final MedicineRepository medicineRepository;

    @Autowired
    public BillingService(BillRepository billRepository,
                          CustomerRepository customerRepository,
                          MedicineRepository medicineRepository) {
        this.billRepository = billRepository;
        this.customerRepository = customerRepository;
        this.medicineRepository = medicineRepository;
    }

    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    public Bill getBillById(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));
    }

    @Transactional
    public Bill createBill(BillRequestDto request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));

        // Pre-validate stock levels
        for (BillItemRequestDto itemDto : request.getItems()) {
            Medicine medicine = medicineRepository.findById(itemDto.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + itemDto.getMedicineId()));

            if (medicine.getStockQuantity() < itemDto.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for " + medicine.getMedicineName() +
                        ". Available stock: " + medicine.getStockQuantity() + ", requested quantity: " + itemDto.getQuantity());
            }
        }

        Bill bill = new Bill();
        bill.setCustomer(customer);
        bill.setBillDate(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (BillItemRequestDto itemDto : request.getItems()) {
            Medicine medicine = medicineRepository.findById(itemDto.getMedicineId()).get();

            BillItem item = new BillItem();
            item.setMedicine(medicine);
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(medicine.getPrice());

            BigDecimal subtotal = medicine.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            item.setSubtotal(subtotal);

            totalAmount = totalAmount.add(subtotal);
            bill.addItem(item);
        }

        bill.setTotalAmount(totalAmount);
        return billRepository.save(bill);
    }
}
