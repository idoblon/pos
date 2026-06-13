package com.springboot.POS.service;

import com.springboot.POS.modal.SubscriptionChangeRequest;

import java.util.List;

public interface SubscriptionChangeRequestService {
    
    SubscriptionChangeRequest createRequest(SubscriptionChangeRequest request);
    
    List<SubscriptionChangeRequest> getAllRequests();
    
    List<SubscriptionChangeRequest> getRequestsByStatus(String status);
    
    List<SubscriptionChangeRequest> getRequestsByStoreId(Long storeId);
    
    SubscriptionChangeRequest approveRequest(Long requestId, Long adminId) throws Exception;
    
    SubscriptionChangeRequest rejectRequest(Long requestId, String reason, Long adminId) throws Exception;
}
