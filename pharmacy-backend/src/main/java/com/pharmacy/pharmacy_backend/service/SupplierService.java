package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.entity.Supplier;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Autowired
    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier getSupplierById(String id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));
    }

    public Supplier createSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public Supplier updateSupplier(String id, Supplier details) {
        Supplier existing = getSupplierById(id);
        existing.setSupplierName(details.getSupplierName());
        existing.setPhone(details.getPhone());
        existing.setEmail(details.getEmail());
        existing.setAddress(details.getAddress());
        return supplierRepository.save(existing);
    }

    public void deleteSupplier(String id) {
        Supplier existing = getSupplierById(id);
        supplierRepository.delete(existing);
    }
}
