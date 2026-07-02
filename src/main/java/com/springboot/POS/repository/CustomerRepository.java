package com.springboot.POS.repository;

import com.springboot.POS.modal.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Store-scoped queries
    List<Customer> findByStoreId(Long storeId);

    Optional<Customer> findByIdAndStoreId(Long id, Long storeId);

    List<Customer> findByStoreIdAndFullNameContainingIgnoreCaseOrderByFullName(Long storeId, String fullName);

    List<Customer> findByStoreIdAndEmailContainingIgnoreCaseOrderByFullName(Long storeId, String email);

    // Admin unscoped queries
    List<Customer> findByFullNameContainingIgnoreCaseOrderByFullName(String fullName);

    List<Customer> findByEmailContainingIgnoreCaseOrderByFullName(String email);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId")
    long countOrdersByCustomerId(@Param("customerId") Long customerId);

}
