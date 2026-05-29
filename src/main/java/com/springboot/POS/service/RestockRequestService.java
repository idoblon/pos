package com.springboot.POS.service;

import com.springboot.POS.domain.RestockStatus;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.RestockRequestDTO;

import java.util.List;

public interface RestockRequestService {

    RestockRequestDTO createRequest(RestockRequestDTO requestDTO, User user) throws Exception;

    List<RestockRequestDTO> getRequestsByStore(Long storeId);

    List<RestockRequestDTO> getRequestsByBranch(Long branchId);

    List<RestockRequestDTO> getRequestsByStoreAndStatus(Long storeId, RestockStatus status);

    RestockRequestDTO approveRequest(Long requestId, User user) throws Exception;

    RestockRequestDTO rejectRequest(Long requestId, String reason, User user) throws Exception;

    RestockRequestDTO fulfillRequest(Long requestId, User user) throws Exception;

    List<RestockRequestDTO> batchApprove(List<Long> requestIds, User user) throws Exception;

    List<RestockRequestDTO> batchReject(List<Long> requestIds, String reason, User user) throws Exception;

    List<RestockRequestDTO> batchFulfill(List<Long> requestIds, User user) throws Exception;
}
