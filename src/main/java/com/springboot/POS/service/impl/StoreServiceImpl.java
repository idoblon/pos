package com.springboot.POS.service.impl;

import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.StoreMapper;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.StoreContact;
import com.springboot.POS.modal.StoreRegistrationRequest;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StoreDTO;
import com.springboot.POS.repository.StoreRegistrationRequestRepository;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.StoreService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreRegistrationRequestRepository registrationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public StoreDTO createStore(StoreDTO storeDTO, User user) {
        Store store = StoreMapper.toEntity(storeDTO, user);
        return StoreMapper.toDTO(storeRepository.save(store));
    }

    @Override
    public StoreDTO getStoreById(Long id) throws Exception {
        Store store = storeRepository.findById(id).orElseThrow(
                () -> new Exception("Store not found with id: " + id));
        StoreDTO dto = StoreMapper.toDTO(store);
        enrichFromRegistration(store, dto);
        return dto;
    }

    @Override
    public List<Store> getAllStores() {
        // This returns entities as per interface
        return storeRepository.findAll().stream()
                .filter(store -> !Boolean.TRUE.equals(store.getDeleted()))
                .collect(Collectors.toList());
    }

    // Optional: Add a method to get DTOs if needed
    @Override
    public List<StoreDTO> getAllStoreDTOs() {
        return storeRepository.findAll().stream()
                .filter(store -> !Boolean.TRUE.equals(store.getDeleted()))
                .map(store -> {
                    StoreDTO dto = StoreMapper.toDTO(store);
                    enrichFromRegistration(store, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void enrichFromRegistration(Store store, StoreDTO dto) {
        StoreRegistrationRequest registration = resolveRegistrationRequest(store);
        if (registration == null) return;

        if (isBlank(dto.getSubscriptionPlan()))    dto.setSubscriptionPlan(registration.getSubscriptionPlan());
        if (dto.getEstimatedBranches() == null)    dto.setEstimatedBranches(registration.getEstimatedBranches());
        if (dto.getEstimatedUsers() == null)       dto.setEstimatedUsers(registration.getEstimatedUsers());
        if (dto.getRegistrationRequestId() == null) dto.setRegistrationRequestId(registration.getId());

        // Resolve address/email/phone from registration only when contact is blank
        String address = dto.getStoreAddress();
        String email   = dto.getEmail();
        String phone   = dto.getPhone();
        if (isBlank(address)) address = registration.getStoreAddress();
        if (isBlank(email))   email   = registration.getEmail();
        if (isBlank(phone))   phone   = registration.getPhone();

        // Owner name: prefer store.fullName, fall back to storeAdmin, then registration
        String fullName = dto.getFullName();
        if (isBlank(fullName) && dto.getStoreAdmin() != null) fullName = dto.getStoreAdmin().getFullName();
        if (isBlank(fullName)) fullName = registration.getOwnerName();

        dto.setFullName(fullName);
        dto.setStoreAddress(address);
        dto.setEmail(email);
        dto.setPhone(phone);

        // Keep contact object in sync
        com.springboot.POS.modal.StoreContact c = new com.springboot.POS.modal.StoreContact();
        c.setAddress(address != null ? address : "");
        c.setEmail(email     != null ? email   : "");
        c.setPhone(phone     != null ? phone   : "");
        dto.setContact(c);
    }

    private StoreRegistrationRequest resolveRegistrationRequest(Store store) {
        if (store.getRegistrationRequestId() != null) {
            StoreRegistrationRequest byRequestId =
                    registrationRepository.findById(store.getRegistrationRequestId()).orElse(null);
            if (byRequestId != null) {
                return byRequestId;
            }
        }
        if (store.getId() != null) {
            StoreRegistrationRequest byStoreId =
                    registrationRepository.findByCreatedStoreId(store.getId()).orElse(null);
            if (byStoreId != null) {
                return byStoreId;
            }
        }
        if (store.getBrand() != null) {
            StoreRegistrationRequest approved =
                    registrationRepository.findFirstByStoreNameIgnoreCaseAndStatusOrderByCreatedAtDesc(
                            store.getBrand(), "APPROVED").orElse(null);
            if (approved != null) {
                return approved;
            }
            return registrationRepository.findFirstByStoreNameIgnoreCaseOrderByCreatedAtDesc(store.getBrand())
                    .orElse(null);
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public Store getStoreByAdmin() throws UserException {
        User admin = userService.getCurrentUser();
        if (admin == null) {
            throw new UserException("User not authenticated");
        }
        Store store = storeRepository.findByStoreAdminId(admin.getId());
        if (store == null) {
            throw new UserException("No store found for current admin");
        }
        return store;
    }

    @Override
    public StoreDTO updateStore(Long id, StoreDTO storeDTO) throws Exception {
        // Implement the update logic here (moved from updatedStore)
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            throw new UserException("User not authenticated");
        }

        // Find store by ID instead of by admin
        Store existing = storeRepository.findById(id)
                .orElseThrow(() -> new Exception("Store not found with id: " + id));

        // Verify that the current user is the admin of this store
        if (!existing.getStoreAdmin().getId().equals(currentUser.getId())) {
            throw new UserException("You can only update your own store");
        }

        // Update fields only if they are provided
        if (storeDTO.getBrand() != null) {
            existing.setBrand(storeDTO.getBrand());
        }

        if (storeDTO.getDescription() != null) {
            existing.setDescription(storeDTO.getDescription());
        }

        if (storeDTO.getStoreType() != null) {
            existing.setStoreType(storeDTO.getStoreType());
        }

        if (storeDTO.getContact() != null
                || storeDTO.getStoreAddress() != null
                || storeDTO.getEmail() != null
                || storeDTO.getPhone() != null) {
            // Resolve from flat fields first, fall back to contact object
            String address = storeDTO.getStoreAddress();
            String email   = storeDTO.getEmail();
            String phone   = storeDTO.getPhone();
            if (address == null && storeDTO.getContact() != null) address = storeDTO.getContact().getAddress();
            if (email   == null && storeDTO.getContact() != null) email   = storeDTO.getContact().getEmail();
            if (phone   == null && storeDTO.getContact() != null) phone   = storeDTO.getContact().getPhone();
            // Keep both columns in sync
            existing.setStoreAddress(address);
            if (storeDTO.getFullName() != null) existing.setFullName(storeDTO.getFullName());
            StoreContact contact = StoreContact.builder()
                    .address(address != null ? address : "")
                    .email(email     != null ? email   : "")
                    .phone(phone     != null ? phone   : "")
                    .build();
            existing.setContact(contact);
        }

        // Update status if provided
        if (storeDTO.getStatus() != null) {
            existing.setStatus(storeDTO.getStatus());
        }

        Store updatedStore = storeRepository.save(existing);
        return StoreMapper.toDTO(updatedStore);
    }

    // Remove the incorrectly named method or keep it for backward compatibility
    @Deprecated
    public StoreDTO updatedStore(Long id, StoreDTO storeDTO) throws Exception {
        return updateStore(id, storeDTO);
    }

    @Override
    @Transactional
    public void deleteStore(Long id) throws UserException {
        try {
            User currentUser = userService.getCurrentUser();

            if (currentUser == null) {
                throw new UserException("User not authenticated");
            }

            // Find store by ID instead of using getStoreByAdmin()
            Store store = storeRepository.findById(id)
                    .orElseThrow(() -> new UserException("Store not found with id: " + id));
            // Platform administrators can manage every registered store. Store
            // administrators remain restricted to deleting only their own store.
            boolean isPlatformAdmin = currentUser.getRole() == UserRole.ROLE_ADMIN;
            boolean isStoreAdmin = store.getStoreAdmin() != null
                    && store.getStoreAdmin().getId().equals(currentUser.getId());
            if (!isPlatformAdmin && !isStoreAdmin) {
                throw new UserException("You can only delete your own store");
            }

            // Detach and deactivate every user that refers to this store before
            // removing it. This prevents a dangling user.store_id foreign key and
            // ensures former staff cannot keep using a deleted store.
            List<User> storeUsers = userRepository.findByStore(store);
            for (User storeUser : storeUsers) {
                storeUser.setStore(null);
                storeUser.setBranch(null);
                storeUser.setDeleted(true);
                storeUser.setStatus("inactive");
            }
            userRepository.saveAll(storeUsers);

            // Preserve orders, payments, products, branches, and configuration for
            // audit purposes. A physical delete would violate one of those foreign
            // keys; a deleted store is hidden from normal store listings instead.
            store.setStoreAdmin(null);
            // Do not change status here: existing databases can carry a legacy
            // status check constraint. The deleted flag is the deletion state.
            store.setDeleted(true);
            storeRepository.save(store);

            // The admin UI also merges registration records into its store list.
            // Mark the originating request as rejected so this deleted store is
            // not reintroduced there, while preserving the record for audit.
            registrationRepository.findByCreatedStoreId(id).ifPresent(request -> {
                request.setStatus("REJECTED");
                request.setRejectionReason("Store deleted by an administrator.");
                registrationRepository.save(request);
            });

        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            throw new UserException("Error deleting store: " + e.getMessage());
        }
    }

    @Override
    public StoreDTO getStoreByEmployee() throws UserException {
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            throw new UserException("User not authenticated");
        }

        Store store = currentUser.getStore();
        if (store == null) {
            throw new UserException("No store associated with current employee");
        }

        return StoreMapper.toDTO(store);
    }

    @Override
    public StoreDTO moderateStore(Long id, StoreStatus status) throws Exception {
        Store store = storeRepository.findById(id).orElseThrow(
                () -> new Exception("Store not found with id: " + id));

        // Only platform-level ROLE_ADMIN can moderate stores
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || !currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new UserException("Only platform admins can moderate stores");
        }

        store.setStatus(status);
        Store updatedStore = storeRepository.save(store);
        return StoreMapper.toDTO(updatedStore);
    }

    @Override
    public Store createStoreFromRegistration(String storeName, String description, String address, String phone, String storeType, String subscriptionPlan, Integer estimatedBranches, Integer estimatedUsers, String ownerName, String email) {
        Store store = new Store();
        store.setBrand(storeName);
        store.setDescription(description);
        store.setStoreType(storeType);
        store.setStatus(StoreStatus.ACTIVE);
        
        // Set subscription information - ensure no NULLs
        store.setSubscriptionPlan(subscriptionPlan != null ? subscriptionPlan.toUpperCase() : "BASIC");
        store.setEstimatedBranches(estimatedBranches != null ? estimatedBranches : 1);
        store.setEstimatedUsers(estimatedUsers != null ? estimatedUsers : 1);
        store.setFullName(ownerName);
        store.setStoreAddress(address);
        
        // CRITICAL: Set contact information - ensure ALL fields are preserved
        StoreContact contact = new StoreContact();
        contact.setAddress(address != null ? address : "");
        contact.setPhone(phone != null ? phone : "");
        contact.setEmail(email != null ? email : "");
        store.setContact(contact);
        
        Store savedStore = storeRepository.save(store);
        
        System.out.println("✅ Store created: " + storeName + 
            " | Email from registration: " + email + 
            " | Contact Email in DB: " + (savedStore.getContact() != null ? savedStore.getContact().getEmail() : "NULL") +
            " | Store ID: " + savedStore.getId());
        
        return savedStore;
    }

    @Override
    public void updateSubscriptionPlan(Long storeId, String subscriptionPlan) throws Exception {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new Exception("Store not found with id: " + storeId));
        
        store.setSubscriptionPlan(subscriptionPlan);
        storeRepository.save(store);
    }

    @Override
    public int backfillSubscriptionDates() {
        List<Store> stores = storeRepository.findAll();
        int updated = 0;
        for (Store store : stores) {
            boolean needsUpdate = store.getSubscriptionPurchaseDate() == null
                    || store.getSubscriptionExpiry() == null
                    || store.getSubscriptionStatus() == null
                    || store.getSubscriptionStatus().isBlank();

            if (!needsUpdate) continue;

            // Determine purchase date: prefer registration processedAt, then approvedAt, then createdAt
            java.time.LocalDateTime purchaseDate = null;
            StoreRegistrationRequest reg = resolveRegistrationRequest(store);
            if (reg != null) {
                purchaseDate = reg.getProcessedAt() != null ? reg.getProcessedAt() : reg.getCreatedAt();
                // Also fix subscription plan from registration if missing
                if (isBlank(store.getSubscriptionPlan()) && !isBlank(reg.getSubscriptionPlan())) {
                    store.setSubscriptionPlan(reg.getSubscriptionPlan().toUpperCase());
                }
            }
            if (purchaseDate == null) {
                purchaseDate = store.getApprovedAt() != null ? store.getApprovedAt() : store.getCreatedAt();
            }
            if (purchaseDate == null) {
                purchaseDate = java.time.LocalDateTime.now();
            }

            if (store.getSubscriptionPurchaseDate() == null) {
                store.setSubscriptionPurchaseDate(purchaseDate);
            }
            if (store.getSubscriptionExpiry() == null) {
                store.setSubscriptionExpiry(store.getSubscriptionPurchaseDate().plusYears(1));
            }
            if (store.getSubscriptionStatus() == null || store.getSubscriptionStatus().isBlank()) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, store.getSubscriptionExpiry());
                if (daysLeft < 0) store.setSubscriptionStatus("EXPIRED");
                else if (daysLeft <= 30) store.setSubscriptionStatus("EXPIRING_SOON");
                else store.setSubscriptionStatus("ACTIVE");
            }
            if (store.getSubscriptionRenewalCount() == null) {
                store.setSubscriptionRenewalCount(0);
            }

            storeRepository.save(store);
            updated++;
        }
        return updated;
    }
}