package com.springboot.POS.repository;

import com.springboot.POS.modal.StoreRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StoreRegistrationRequestRepository extends JpaRepository<StoreRegistrationRequest, Long> {
    List<StoreRegistrationRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<StoreRegistrationRequest> findAllByOrderByCreatedAtDesc();
    Optional<StoreRegistrationRequest> findByEmail(String email);
    Optional<StoreRegistrationRequest> findByCreatedStoreId(Long createdStoreId);
    Optional<StoreRegistrationRequest> findFirstByStoreNameIgnoreCaseAndStatusOrderByCreatedAtDesc(String storeName, String status);
    Optional<StoreRegistrationRequest> findFirstByStoreNameIgnoreCaseOrderByCreatedAtDesc(String storeName);
    long countByStatus(String status);
    boolean existsByEmail(String email);
}
