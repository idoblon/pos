package com.springboot.POS.mapper;

import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StoreDTO;

public class StoreMapper {

    public static StoreDTO toDTO(Store store){
        StoreDTO storeDTO =  new StoreDTO();
        storeDTO.setId(store.getId());
        storeDTO.setBrand(store.getBrand());
        storeDTO.setDescription(store.getDescription());
        storeDTO.setStoreAdmin(UserMapper.toDTO(store.getStoreAdmin()));
        storeDTO.setStoreType(store.getStoreType());
        storeDTO.setCreatedAt(store.getCreatedAt());
        storeDTO.setUpdatedAt(store.getUpdatedAt());
        storeDTO.setStatus(store.getStatus());
        
        // Map registration and subscription fields
        storeDTO.setFullName(store.getFullName());
        storeDTO.setEmail(store.getContact() != null ? store.getContact().getEmail() : null);
        storeDTO.setPhone(store.getContact() != null ? store.getContact().getPhone() : null);
        storeDTO.setStoreAddress(store.getStoreAddress());
        storeDTO.setSubscriptionPlan(store.getSubscriptionPlan());
        storeDTO.setEstimatedBranches(store.getEstimatedBranches());
        storeDTO.setEstimatedUsers(store.getEstimatedUsers());
        storeDTO.setTotalRevenue(store.getTotalRevenue());
        storeDTO.setApprovedAt(store.getApprovedAt());
        storeDTO.setRegistrationRequestId(store.getRegistrationRequestId());
        
        // Include contact information
        storeDTO.setContact(store.getContact());

        return storeDTO;
    }

    public static Store toEntity(StoreDTO storeDTO, User storeAdmin){
        Store store = new Store();
        store.setId(storeDTO.getId());
        store.setBrand(storeDTO.getBrand());
        store.setDescription(storeDTO.getDescription());
        store.setStoreAdmin(storeAdmin);
        store.setStoreType(storeDTO.getStoreType());
        store.setContact(storeDTO.getContact());
        
        // Map registration and subscription fields from DTO
        store.setFullName(storeDTO.getFullName());
        // Set email and phone through contact object
        if (store.getContact() == null) {
            store.setContact(new com.springboot.POS.modal.StoreContact());
        }
        if (storeDTO.getEmail() != null) {
            store.getContact().setEmail(storeDTO.getEmail());
        }
        if (storeDTO.getPhone() != null) {
            store.getContact().setPhone(storeDTO.getPhone());
        }
        store.setStoreAddress(storeDTO.getStoreAddress());
        store.setSubscriptionPlan(storeDTO.getSubscriptionPlan());
        store.setEstimatedBranches(storeDTO.getEstimatedBranches());
        store.setEstimatedUsers(storeDTO.getEstimatedUsers());
        store.setTotalRevenue(storeDTO.getTotalRevenue());
        store.setApprovedAt(storeDTO.getApprovedAt());
        store.setRegistrationRequestId(storeDTO.getRegistrationRequestId());
        
        return store;
    }


}
