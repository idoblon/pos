package com.springboot.POS.controller;

import com.springboot.POS.domain.RestockStatus;
import com.springboot.POS.modal.RestockRequest;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.RestockRequestDTO;
import com.springboot.POS.service.RestockRequestService;
import com.springboot.POS.repository.RestockRequestRepository;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/restock-requests")
public class RestockRequestController {

    private final RestockRequestService restockRequestService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;
    private final RestockRequestRepository restockRequestRepository;

    /** Scope check: the request must belong to the requester's own branch/store. */
    private void guardRequest(User user, Long requestId) throws Exception {
        RestockRequest request = restockRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Restock request not found"));
        if (request.getBranch() != null) {
            ownershipGuard.requireBranchAccess(user, request.getBranch().getId());
        }
    }

    @PostMapping
    public ResponseEntity<RestockRequestDTO> createRequest(
            @RequestBody @Valid RestockRequestDTO requestDTO,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.createRequest(requestDTO, user));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<RestockRequestDTO>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) RestockStatus status,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);

        if (status != null) {
            return ResponseEntity.ok(restockRequestService.getRequestsByStoreAndStatus(storeId, status));
        }
        return ResponseEntity.ok(restockRequestService.getRequestsByStore(storeId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<RestockRequestDTO>> getByBranch(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(restockRequestService.getRequestsByBranch(branchId));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<RestockRequestDTO> approve(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        guardRequest(user, id);
        return ResponseEntity.ok(restockRequestService.approveRequest(id, user));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<RestockRequestDTO> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        guardRequest(user, id);
        String reason = body.get("reason");
        return ResponseEntity.ok(restockRequestService.rejectRequest(id, reason, user));
    }

    @PatchMapping("/{id}/fulfill")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<RestockRequestDTO> fulfill(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Integer> body,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        guardRequest(user, id);
        Integer receivedQuantity = (body != null) ? body.get("receivedQuantity") : null;
        return ResponseEntity.ok(restockRequestService.fulfillRequest(id, receivedQuantity, user));
    }

    @PostMapping("/batch/approve")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<List<RestockRequestDTO>> batchApprove(
            @RequestBody List<Long> requestIds,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        for (Long requestId : requestIds) guardRequest(user, requestId);
        return ResponseEntity.ok(restockRequestService.batchApprove(requestIds, user));
    }

    @PostMapping("/batch/reject")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<List<RestockRequestDTO>> batchReject(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        @SuppressWarnings("unchecked")
        List<Long> requestIds = (List<Long>) body.get("requestIds");
        String reason = (String) body.get("reason");
        if (requestIds != null) {
            for (Long requestId : requestIds) guardRequest(user, requestId);
        }
        return ResponseEntity.ok(restockRequestService.batchReject(requestIds, reason, user));
    }

    @PostMapping("/batch/fulfill")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<List<RestockRequestDTO>> batchFulfill(
            @RequestBody List<Long> requestIds,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        for (Long requestId : requestIds) guardRequest(user, requestId);
        return ResponseEntity.ok(restockRequestService.batchFulfill(requestIds, user));
    }
}
