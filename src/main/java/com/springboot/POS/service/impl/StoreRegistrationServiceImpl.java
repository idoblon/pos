package com.springboot.POS.service.impl;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.StoreRegistrationRequest;
import com.springboot.POS.modal.SubscriptionPayment;
import com.springboot.POS.modal.User;
import com.springboot.POS.repository.StoreRegistrationRequestRepository;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.service.EmailService;
import com.springboot.POS.service.PaymentService;
import com.springboot.POS.service.StorePaymentConfigService;
import com.springboot.POS.service.StoreRegistrationService;
import com.springboot.POS.service.StoreService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreRegistrationServiceImpl implements StoreRegistrationService {

    private final StoreRegistrationRequestRepository registrationRepository;
    private final StoreRepository storeRepository;
    private final StoreService storeService;
    private final UserService userService;
    private final EmailService emailService;
    private final PaymentService paymentService;
    private final StorePaymentConfigService storePaymentConfigService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<StoreRegistrationRequest> getAllPendingRequests() {
        return registrationRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    @Override
    public List<StoreRegistrationRequest> getAllRequests() {
        return registrationRepository.findAll();
    }

    @Override
    public StoreRegistrationRequest getRequestById(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration request not found"));
    }

    @Override
    public void approveRequest(Long requestId, Long adminId) throws Exception {
        StoreRegistrationRequest request = getRequestById(requestId);
        
        // Validate request status
        if (!"PENDING".equals(request.getStatus())) {
            throw new Exception("Request has already been processed. Current status: " + request.getStatus());
        }
        
        // Update registration request to PAYMENT_PENDING (not fully approved yet)
        request.setStatus("PAYMENT_PENDING");
        request.setProcessedAt(LocalDateTime.now());
        request.setApprovedByAdminId(adminId);
        
        registrationRepository.save(request);
        
        // Send approval email (NOT credentials email)
        emailService.sendStoreRegistrationApprovalNotification(
            request.getEmail(),
            request.getOwnerName(),
            request.getStoreName(),
            request.getSubscriptionPlan()
        );
    }

    @Override
    public void approveRequestWithOverride(Long requestId, Long adminId, boolean skipPaymentCheck) throws Exception {
        StoreRegistrationRequest request = getRequestById(requestId);
        
        // Validate request status
        if (!"PENDING".equals(request.getStatus()) && !"PAYMENT_PENDING".equals(request.getStatus())) {
            throw new Exception("Request has already been processed. Current status: " + request.getStatus());
        }

        if (!skipPaymentCheck && !paymentService.hasValidPayment(requestId)) {
            throw new Exception("Cannot approve request: Payment not completed.");
        }

        try {
            Store store = storeService.createStoreFromRegistration(
                request.getStoreName(), 
                request.getStoreDescription(),
                request.getStoreAddress(), 
                request.getPhone(), 
                request.getStoreType(),
                request.getSubscriptionPlan(),
                request.getEstimatedBranches() != null ? request.getEstimatedBranches() : 1,
                request.getEstimatedUsers() != null ? request.getEstimatedUsers() : 1,
                request.getOwnerName(),
                request.getEmail()
            );
            store.setRegistrationRequestId(requestId);
            LocalDateTime approvedAt = LocalDateTime.now();
            store.setApprovedAt(approvedAt);
            // Set subscription dates from payment or approval time
            SubscriptionPayment payment = paymentService.getPaymentByRegistrationId(requestId);
            LocalDateTime purchaseDate = (payment != null && payment.getPaidAt() != null)
                    ? payment.getPaidAt() : approvedAt;
            store.setSubscriptionPurchaseDate(purchaseDate);
            store.setSubscriptionExpiry(purchaseDate.plusYears(1));
            store.setSubscriptionStatus("ACTIVE");
            store.setSubscriptionRenewalCount(0);
            User storeAdmin = userService.createStoreAdminWithEncodedPassword(
                request.getOwnerName(), request.getEmail(), request.getPassword(), store
            );
            store.setStoreAdmin(storeAdmin);
            storeRepository.save(store);
            storePaymentConfigService.initializeDefaultPaymentMethods(store.getId());
            request.setStatus("APPROVED");
            request.setProcessedAt(LocalDateTime.now());
            if (adminId != null) request.setApprovedByAdminId(adminId);
            request.setCreatedStoreId(store.getId());
            request.setCreatedUserId(storeAdmin.getId());
            registrationRepository.save(request);
            emailService.sendStoreRegistrationApproved(
                request.getEmail(), request.getOwnerName(), request.getStoreName(),
                request.getEmail()
            );
        } catch (Exception e) {
            throw new Exception("Failed to approve registration: " + e.getMessage(), e);
        }
    }

    @Override
    public void rejectRequest(Long requestId, String reason, Long adminId) throws Exception {
        StoreRegistrationRequest request = getRequestById(requestId);
        
        if (!"PENDING".equals(request.getStatus()) && !"PAYMENT_PENDING".equals(request.getStatus())) {
            throw new Exception("Request has already been processed");
        }

        // Update registration request
        request.setStatus("REJECTED");
        request.setRejectionReason(reason);
        request.setProcessedAt(LocalDateTime.now());
        request.setApprovedByAdminId(adminId);
        
        registrationRepository.save(request);
        
        // Send rejection email
        emailService.sendStoreRegistrationRejected(
            request.getEmail(),
            request.getOwnerName(),
            request.getStoreName(),
            reason
        );
    }
    
}