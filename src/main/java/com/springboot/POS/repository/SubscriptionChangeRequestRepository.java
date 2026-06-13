package com.springboot.POS.repository;

import com.springboot.POS.modal.SubscriptionChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionChangeRequestRepository extends JpaRepository<SubscriptionChangeRequest, Long> {
    
    List<SubscriptionChangeRequest> findByStatus(String status);
    
    List<SubscriptionChangeRequest> findByStoreId(Long storeId);
    
    List<SubscriptionChangeRequest> findByStoreIdOrderByCreatedAtDesc(Long storeId);
}
