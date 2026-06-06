package com.springboot.POS.controller;

import com.springboot.POS.modal.StoreRegistrationRequest;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.StoreRegistrationService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/registration-requests")
public class RegistrationRequestController {

    private final StoreRegistrationService registrationService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StoreRegistrationRequest>> getAllRequests(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User admin = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(registrationService.getAllRequests());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StoreRegistrationRequest>> getPendingRequests(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User admin = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(registrationService.getAllPendingRequests());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreRegistrationRequest> getRequestById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User admin = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(registrationService.getRequestById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> approveRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        registrationService.approveRequest(id, admin.getId());
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Store registration request approved successfully. Approval email sent to applicant.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> rejectRequest(
            @PathVariable Long id,
            @RequestBody RejectRequest rejectRequest,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        registrationService.rejectRequest(id, rejectRequest.getReason(), admin.getId());
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Store registration request rejected successfully. Rejection email sent to applicant.");
        return ResponseEntity.ok(response);
    }

    // Inner class for reject request payload
    public static class RejectRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}