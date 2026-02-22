package com.springboot.POS.service.impl;

import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.StoreMapper;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.StoreContact;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StoreDTO;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.service.StoreService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
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
        return StoreMapper.toDTO(store);
    }

    @Override
    public List<Store> getAllStores() {
        // This returns entities as per interface
        return storeRepository.findAll();
    }

    // Optional: Add a method to get DTOs if needed
    public List<StoreDTO> getAllStoreDTOs() {
        return storeRepository.findAll().stream()
                .map(StoreMapper::toDTO)
                .collect(Collectors.toList());
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

        if (storeDTO.getContact() != null) {
            StoreContact contact = StoreContact.builder()
                    .address(storeDTO.getContact().getAddress())
                    .phone(storeDTO.getContact().getPhone())
                    .email(storeDTO.getContact().getEmail())
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
    public void deleteStore(Long id) throws UserException {
        try {
            User currentUser = userService.getCurrentUser();

            if (currentUser == null) {
                throw new UserException("User not authenticated");
            }

            // Find store by ID instead of using getStoreByAdmin()
            Store store = storeRepository.findById(id)
                    .orElseThrow(() -> new UserException("Store not found with id: " + id));

            // Verify that the current user is the admin of this store
            if (!store.getStoreAdmin().getId().equals(currentUser.getId())) {
                throw new UserException("You can only delete your own store");
            }

            storeRepository.delete(store);

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
}