package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.entity.Customer;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer details) {
        Customer existing = getCustomerById(id);
        existing.setCustomerName(details.getCustomerName());
        existing.setPhone(details.getPhone());
        existing.setEmail(details.getEmail());
        return customerRepository.save(existing);
    }

    public void deleteCustomer(Long id) {
        Customer existing = getCustomerById(id);
        customerRepository.delete(existing);
    }
}
