package com.springboot.POS.service.impl;

import com.springboot.POS.modal.SubscriptionChangeRequest;
import com.springboot.POS.repository.SubscriptionChangeRequestRepository;
import com.springboot.POS.service.StoreService;
import com.springboot.POS.service.SubscriptionChangeRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionChangeRequestServiceImpl implements SubscriptionChangeRequestService {
    
    private final SubscriptionChangeRequestRepository repository;
    private final StoreService storeService;
    
    @Override
    @Transactional
    public SubscriptionChangeRequest createRequest(SubscriptionChangeRequest request) {
        log.info("Creating subscription change request for store: {}, {} from {} to {}", 
            request.getStoreId(), request.getChangeType(), request.getCurrentPlan(), request.getRequestedPlan());
        return repository.save(request);
    }
    
    @Override
    public List<SubscriptionChangeRequest> getAllRequests() {
        return repository.findAll();
    }
    
    @Override
    public List<SubscriptionChangeRequest> getRequestsByStatus(String status) {
        return repository.findByStatus(status);
    }
    
    @Override
    public List<SubscriptionChangeRequest> getRequestsByStoreId(Long storeId) {
        return repository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }
    
    @Override
    @Transactional
    public SubscriptionChangeRequest approveRequest(Long requestId, Long adminId) throws Exception {
        SubscriptionChangeRequest request = repository.findById(requestId)
            .orElseThrow(() -> new Exception("Subscription change request not found"));
        
        if ("APPROVED".equals(request.getStatus())) {
            throw new Exception("Request already approved");
        }
        
        if ("REJECTED".equals(request.getStatus())) {
            throw new Exception("Cannot approve a rejected request");
        }
        
        request.setStatus("APPROVED");
        request.setApprovedAt(LocalDateTime.now());
        request.setApprovedBy(adminId);
        
        storeService.updateSubscriptionPlan(request.getStoreId(), request.getRequestedPlan());
        
        log.info("Subscription change request {} approved by admin {}", requestId, adminId);
        return repository.save(request);
    }
    
    @Override
    @Transactional
    public SubscriptionChangeRequest rejectRequest(Long requestId, String reason, Long adminId) throws Exception {
        SubscriptionChangeRequest request = repository.findById(requestId)
            .orElseThrow(() -> new Exception("Subscription change request not found"));
        
        if ("APPROVED".equals(request.getStatus())) {
            throw new Exception("Cannot reject an approved request");
        }
        
        if ("REJECTED".equals(request.getStatus())) {
            throw new Exception("Request already rejected");
        }
        
        request.setStatus("REJECTED");
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectedBy(adminId);
        request.setRejectionReason(reason);
        
        log.info("Subscription change request {} rejected by admin {}: {}", requestId, adminId, reason);
        return repository.save(request);
    }
}
