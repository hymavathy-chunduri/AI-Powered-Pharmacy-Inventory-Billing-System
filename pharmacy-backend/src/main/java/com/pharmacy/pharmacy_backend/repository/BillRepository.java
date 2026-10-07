package com.pharmacy.pharmacy_backend.repository;

import com.pharmacy.pharmacy_backend.entity.Bill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillRepository extends MongoRepository<Bill, String> {
}
