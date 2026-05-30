package com.springboot.POS.repository;

import com.springboot.POS.modal.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName, String email);

    List<Customer> findByFullNameContainingIgnoreCaseOrderByFullName(String fullName);
    
    List<Customer> findByEmailContainingIgnoreCaseOrderByFullName(String email);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId")
    long countOrdersByCustomerId(@Param("customerId") Long customerId);

}
