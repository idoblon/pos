package com.springboot.POS.service;

import com.springboot.POS.modal.StoreRegistrationRequest;

import java.util.List;

public interface StoreRegistrationService {
    List<StoreRegistrationRequest> getAllPendingRequests();
    List<StoreRegistrationRequest> getAllRequests();
    StoreRegistrationRequest getRequestById(Long id);
    void approveRequest(Long requestId, Long adminId) throws Exception;
    void approveRequestWithOverride(Long requestId, Long adminId, boolean skipPaymentCheck) throws Exception;
    void rejectRequest(Long requestId, String reason, Long adminId) throws Exception;
}