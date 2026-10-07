package com.pharmacy.pharmacy_backend.repository;

import com.pharmacy.pharmacy_backend.entity.Purchase;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseRepository extends MongoRepository<Purchase, String> {
}
