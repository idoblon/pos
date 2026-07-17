package com.springboot.POS.service;

import com.springboot.POS.payload.dto.StoreRegistrationRequestDTO;
import java.util.List;

public interface StoreRegistrationService {
    StoreRegistrationRequestDTO submitRequest(StoreRegistrationRequestDTO dto) throws Exception;
    List<StoreRegistrationRequestDTO> getRequestsByStatus(String status);
    List<StoreRegistrationRequestDTO> getAllRequests();
    List<StoreRegistrationRequestDTO> getAllPendingRequests();
    StoreRegistrationRequestDTO approveRequest(Long requestId, Long adminId) throws Exception;
    StoreRegistrationRequestDTO approveRequestWithOverride(Long requestId, Long adminId, boolean override) throws Exception;
    StoreRegistrationRequestDTO rejectRequest(Long requestId, String reason, Long adminId) throws Exception;
    long getPendingCount();
    StoreRegistrationRequestDTO getRequestById(Long id) throws Exception;
}
