package com.pharmacy.pharmacy_backend.repository;

import com.pharmacy.pharmacy_backend.entity.Bill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillRepository extends MongoRepository<Bill, String> {

    /** Returns the most recent N bills — avoids loading all 270+ bills for API listing */
    List<Bill> findAllByOrderByBillDateDesc(Pageable pageable);
}
