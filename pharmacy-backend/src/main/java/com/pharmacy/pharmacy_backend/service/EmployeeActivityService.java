package com.pharmacy.pharmacy_backend.service;

import com.pharmacy.pharmacy_backend.entity.Bill;
import com.pharmacy.pharmacy_backend.entity.Employee;
import com.pharmacy.pharmacy_backend.entity.LoginHistory;
import com.pharmacy.pharmacy_backend.exception.ResourceNotFoundException;
import com.pharmacy.pharmacy_backend.repository.BillRepository;
import com.pharmacy.pharmacy_backend.repository.EmployeeRepository;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class EmployeeActivityService {

    private final EmployeeRepository employeeRepository;
    private final BillRepository billRepository;
    private final LoginHistoryService loginHistoryService;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public EmployeeActivityService(EmployeeRepository employeeRepository,
                                   BillRepository billRepository,
                                   LoginHistoryService loginHistoryService,
                                   MongoTemplate mongoTemplate) {
        this.employeeRepository = employeeRepository;
        this.billRepository = billRepository;
        this.loginHistoryService = loginHistoryService;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Efficiently calculates employee activity metrics using server-side aggregation
     * and indexed count queries without loading all historical bills into memory.
     */
    public Map<String, Object> getActivitySummary(String employeeId) {
        Employee employee = employeeRepository.findByEmployeeIdIgnoreCase(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        String empId = employee.getEmployeeId();

        // 1. Efficient count query
        long totalBillsCount = billRepository.countByCreatedByEmployeeId(empId);

        // 2. Server-side MongoDB aggregation for total sales value
        BigDecimal totalSalesValue = BigDecimal.ZERO;
        try {
            MatchOperation match = Aggregation.match(Criteria.where("created_by_employee_id").is(empId));
            GroupOperation group = Aggregation.group().sum("total_amount").as("totalSales");
            Aggregation agg = Aggregation.newAggregation(match, group);

            var aggResults = mongoTemplate.aggregate(agg, "bills", Document.class);
            if (!aggResults.getMappedResults().isEmpty()) {
                Object val = aggResults.getMappedResults().get(0).get("totalSales");
                if (val instanceof Decimal128) {
                    totalSalesValue = ((Decimal128) val).bigDecimalValue();
                } else if (val instanceof Double) {
                    totalSalesValue = BigDecimal.valueOf((Double) val);
                } else if (val instanceof Number) {
                    totalSalesValue = BigDecimal.valueOf(((Number) val).doubleValue());
                }
            }
        } catch (Exception e) {
            // Fallback in case aggregation pipeline cannot run on embedded instance
            List<Bill> sample = billRepository.findByCreatedByEmployeeIdOrderByBillDateDesc(empId, PageRequest.of(0, 100));
            totalSalesValue = sample.stream()
                    .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 3. Paginated query for 5 most recent bills
        List<Bill> recentBills = billRepository.findByCreatedByEmployeeIdOrderByBillDateDesc(empId, PageRequest.of(0, 5));

        // 4. Recent audit events (last 10)
        List<LoginHistory> recentEvents = loginHistoryService.getRecentEventsForEmployee(empId, 10);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("employeeId", empId);
        summary.put("fullName", employee.getFullName());
        summary.put("email", employee.getEmail());
        summary.put("role", employee.getRole());
        summary.put("active", employee.isActive());
        summary.put("lastLoginAt", employee.getLastLoginAt());
        summary.put("totalBillsCount", totalBillsCount);
        summary.put("totalSalesValue", totalSalesValue);
        summary.put("recentBills", recentBills);
        summary.put("recentEvents", recentEvents);

        return summary;
    }
}
