package com.springboot.POS.repository;

import com.springboot.POS.modal.StoreRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRegistrationRequestRepository extends JpaRepository<StoreRegistrationRequest, Long> {
    Optional<StoreRegistrationRequest> findByEmail(String email);
    boolean existsByEmail(String email);
    List<StoreRegistrationRequest> findByStatus(String status);
    List<StoreRegistrationRequest> findByStatusOrderByCreatedAtDesc(String status);
    Optional<StoreRegistrationRequest> findByCreatedStoreId(Long createdStoreId);
    Optional<StoreRegistrationRequest> findFirstByStoreNameIgnoreCaseAndStatusOrderByCreatedAtDesc(String storeName, String status);
    Optional<StoreRegistrationRequest> findFirstByStoreNameIgnoreCaseOrderByCreatedAtDesc(String storeName);
}