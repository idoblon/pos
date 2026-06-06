package com.springboot.POS.service.impl;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.StoreRegistrationRequest;
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

import java.security.SecureRandom;
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
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new Exception("Request has already been processed");
        }
        
        // Check if payment is completed
        if (!paymentService.hasValidPayment(requestId)) {
            throw new Exception("Cannot approve request: Payment not completed. Please ensure the user has completed the subscription payment.");
        }

        try {
            // Generate temporary password
            String tempPassword = generateTempPassword();
            
            // Create store
            Store store = storeService.createStoreFromRegistration(
                request.getStoreName(),
                request.getStoreDescription(),
                request.getStoreAddress(),
                request.getPhone(),
                request.getStoreType()
            );
            
            // Create store admin user
            User storeAdmin = userService.createStoreAdmin(
                request.getOwnerName(),
                request.getEmail(),
                tempPassword,
                store
            );
            
            // Update store with admin reference
            store.setStoreAdmin(storeAdmin);
            storeRepository.save(store);
            
            // Initialize default payment methods for the store
            storePaymentConfigService.initializeDefaultPaymentMethods(store.getId());
            
            // Update registration request
            request.setStatus("APPROVED");
            request.setProcessedAt(LocalDateTime.now());
            request.setApprovedByAdminId(adminId);
            request.setCreatedStoreId(store.getId());
            request.setCreatedUserId(storeAdmin.getId());
            
            registrationRepository.save(request);
            
            // Send approval email
            emailService.sendStoreRegistrationApproved(
                request.getEmail(),
                request.getOwnerName(),
                request.getStoreName(),
                request.getEmail(),
                tempPassword
            );
            
        } catch (Exception e) {
            throw new Exception("Failed to approve registration: " + e.getMessage());
        }
    }

    @Override
    public void rejectRequest(Long requestId, String reason, Long adminId) throws Exception {
        StoreRegistrationRequest request = getRequestById(requestId);
        
        if (!"PENDING".equals(request.getStatus())) {
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
    
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
}