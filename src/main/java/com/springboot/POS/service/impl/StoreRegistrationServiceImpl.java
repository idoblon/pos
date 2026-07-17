package com.springboot.POS.service.impl;

import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.domain.UserRole;
import com.springboot.POS.modal.*;
import com.springboot.POS.payload.dto.StoreRegistrationRequestDTO;
import com.springboot.POS.repository.*;
import com.springboot.POS.service.EmailService;
import com.springboot.POS.service.StoreRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreRegistrationServiceImpl implements StoreRegistrationService {

    private final StoreRegistrationRequestRepository requestRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.admin.email:posproofficial@gmail.com}")
    private String adminEmail;

    @Override
    @Transactional
    public StoreRegistrationRequestDTO submitRequest(StoreRegistrationRequestDTO dto) throws Exception {
        if (requestRepository.existsByEmail(dto.getEmail())) {
            throw new Exception("A registration request with this email already exists.");
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new Exception("An account with this email already exists.");
        }

        StoreRegistrationRequest request = new StoreRegistrationRequest();
        request.setOwnerName(dto.getOwnerName());
        request.setEmail(dto.getEmail());
        request.setPhone(dto.getPhone());
        request.setPassword(passwordEncoder.encode(dto.getPassword()));
        request.setStoreName(dto.getStoreName());
        request.setStoreDescription(dto.getStoreDescription());
        request.setStoreType(dto.getStoreType());
        request.setStoreAddress(dto.getStoreAddress());
        request.setSubscriptionPlan(dto.getSubscriptionPlan() != null ? dto.getSubscriptionPlan() : "BASIC");
        request.setStatus("PENDING");

        StoreRegistrationRequest saved = requestRepository.save(request);

        // Notify admin
        try {
            emailService.sendStoreRegistrationNotification(
                adminEmail,
                saved.getStoreName(),
                saved.getOwnerName(),
                saved.getEmail(),
                saved.getSubscriptionPlan()
            );
        } catch (Exception e) {
            // Don't fail the request if email fails
        }

        return toDTO(saved);
    }

    @Override
    public List<StoreRegistrationRequestDTO> getRequestsByStatus(String status) {
        return requestRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<StoreRegistrationRequestDTO> getAllRequests() {
        return requestRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StoreRegistrationRequestDTO approveRequest(Long requestId, Long adminId) throws Exception {
        StoreRegistrationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Registration request not found: " + requestId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new Exception("Request is already " + request.getStatus());
        }

        // 1. Create Store
        Store store = new Store();
        store.setBrand(request.getStoreName());
        store.setDescription(request.getStoreDescription());
        store.setStoreType(request.getStoreType() != null ? request.getStoreType() : "RETAIL");
        store.setStatus(StoreStatus.ACTIVE);
        store.setSubscriptionPlan(request.getSubscriptionPlan());
        store.setSubscriptionStatus("ACTIVE");
        store.setSubscriptionPurchaseDate(LocalDateTime.now());
        store.setSubscriptionExpiry(LocalDateTime.now().plusYears(1));
        store.setFullName(request.getOwnerName());
        store.setStoreAddress(request.getStoreAddress());
        store.setRegistrationRequestId(requestId);
        store.setApprovedAt(LocalDateTime.now());

        StoreContact contact = new StoreContact();
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setAddress(request.getStoreAddress() != null ? request.getStoreAddress() : "");
        store.setContact(contact);

        Store savedStore = storeRepository.save(store);

        // 2. Create User (ROLE_STORE_ADMIN)
        User user = new User();
        user.setFullName(request.getOwnerName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(request.getPassword()); // Already encoded
        user.setRole(UserRole.ROLE_STORE_ADMIN);
        user.setStore(savedStore);
        user.setStatus("active");

        User savedUser = userRepository.save(user);

        // 3. Link store admin back to store
        savedStore.setStoreAdmin(savedUser);
        storeRepository.save(savedStore);

        // 4. Update request
        request.setStatus("APPROVED");
        request.setProcessedAt(LocalDateTime.now());
        request.setApprovedByAdminId(adminId);
        request.setCreatedStoreId(savedStore.getId());
        request.setCreatedUserId(savedUser.getId());
        requestRepository.save(request);

        // 5. Send approval email with login credentials
        try {
            emailService.sendStoreRegistrationApproved(
                request.getEmail(),
                request.getOwnerName(),
                request.getStoreName(),
                request.getEmail()
            );
        } catch (Exception e) {
            // Don't fail if email fails
        }

        return toDTO(request);
    }

    @Override
    @Transactional
    public StoreRegistrationRequestDTO rejectRequest(Long requestId, String reason, Long adminId) throws Exception {
        StoreRegistrationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Registration request not found: " + requestId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new Exception("Request is already " + request.getStatus());
        }

        request.setStatus("REJECTED");
        request.setRejectionReason(reason);
        request.setProcessedAt(LocalDateTime.now());
        request.setApprovedByAdminId(adminId);
        requestRepository.save(request);

        // Send rejection email
        try {
            emailService.sendStoreRegistrationRejected(
                request.getEmail(),
                request.getOwnerName(),
                request.getStoreName(),
                reason
            );
        } catch (Exception e) {
            // Don't fail if email fails
        }

        return toDTO(request);
    }

    @Override
    public List<StoreRegistrationRequestDTO> getAllPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtDesc("PENDING")
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public StoreRegistrationRequestDTO approveRequestWithOverride(Long requestId, Long adminId, boolean override) throws Exception {
        return approveRequest(requestId, adminId);
    }

    @Override
    public StoreRegistrationRequestDTO getRequestById(Long id) throws Exception {
        StoreRegistrationRequest r = requestRepository.findById(id)
                .orElseThrow(() -> new Exception("Registration request not found: " + id));
        return toDTO(r);
    }

    @Override
    public long getPendingCount() {
        return requestRepository.countByStatus("PENDING");
    }

    private StoreRegistrationRequestDTO toDTO(StoreRegistrationRequest r) {
        return StoreRegistrationRequestDTO.builder()
                .id(r.getId())
                .ownerName(r.getOwnerName())
                .email(r.getEmail())
                .phone(r.getPhone())
                .storeName(r.getStoreName())
                .storeDescription(r.getStoreDescription())
                .storeType(r.getStoreType())
                .storeAddress(r.getStoreAddress())
                .subscriptionPlan(r.getSubscriptionPlan())
                .status(r.getStatus())
                .rejectionReason(r.getRejectionReason())
                .estimatedBranches(r.getEstimatedBranches())
                .estimatedUsers(r.getEstimatedUsers())
                .createdAt(r.getCreatedAt())
                .processedAt(r.getProcessedAt())
                .createdStoreId(r.getCreatedStoreId())
                .createdUserId(r.getCreatedUserId())
                .build();
    }
}
