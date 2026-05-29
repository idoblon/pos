package com.springboot.POS.controller;

import com.springboot.POS.domain.RestockStatus;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.RestockRequestDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.RestockRequestService;
import com.springboot.POS.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/restock-requests")
public class RestockRequestController {

    private final RestockRequestService restockRequestService;
    private final UserService userService;

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
        return ResponseEntity.ok(restockRequestService.getRequestsByBranch(branchId));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<RestockRequestDTO> approve(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.approveRequest(id, user));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<RestockRequestDTO> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        String reason = body.get("reason");
        return ResponseEntity.ok(restockRequestService.rejectRequest(id, reason, user));
    }

    @PatchMapping("/{id}/fulfill")
    public ResponseEntity<RestockRequestDTO> fulfill(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.fulfillRequest(id, user));
    }

    @PostMapping("/batch/approve")
    public ResponseEntity<List<RestockRequestDTO>> batchApprove(
            @RequestBody List<Long> requestIds,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.batchApprove(requestIds, user));
    }

    @PostMapping("/batch/reject")
    public ResponseEntity<List<RestockRequestDTO>> batchReject(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        @SuppressWarnings("unchecked")
        List<Long> requestIds = (List<Long>) body.get("requestIds");
        String reason = (String) body.get("reason");
        return ResponseEntity.ok(restockRequestService.batchReject(requestIds, reason, user));
    }

    @PostMapping("/batch/fulfill")
    public ResponseEntity<List<RestockRequestDTO>> batchFulfill(
            @RequestBody List<Long> requestIds,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.batchFulfill(requestIds, user));
    }
}
