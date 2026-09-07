package com.springboot.POS.mapper;

import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StoreDTO;

public class StoreMapper {

    public static StoreDTO toDTO(Store store){
        StoreDTO storeDTO = new StoreDTO();
        storeDTO.setId(store.getId());
        storeDTO.setBrand(store.getBrand());
        storeDTO.setDescription(store.getDescription());
        storeDTO.setStoreAdmin(UserMapper.toDTO(store.getStoreAdmin()));
        storeDTO.setStoreType(store.getStoreType());
        storeDTO.setCreatedAt(store.getCreatedAt());
        storeDTO.setUpdatedAt(store.getUpdatedAt());
        storeDTO.setStatus(store.getStatus());

        // Single source of truth: contact embedded object
        com.springboot.POS.modal.StoreContact c = store.getContact();
        String contactEmail   = c != null ? c.getEmail()   : null;
        String contactPhone   = c != null ? c.getPhone()   : null;
        String contactAddress = c != null ? c.getAddress() : null;

        // Flat convenience fields — prefer contact, fall back to legacy columns
        storeDTO.setEmail(contactEmail   != null && !contactEmail.isBlank()   ? contactEmail   : null);
        storeDTO.setPhone(contactPhone   != null && !contactPhone.isBlank()   ? contactPhone   : null);
        storeDTO.setStoreAddress(contactAddress != null && !contactAddress.isBlank() ? contactAddress : store.getStoreAddress());
        storeDTO.setFullName(store.getFullName() != null ? store.getFullName()
                : (store.getStoreAdmin() != null ? store.getStoreAdmin().getFullName() : null));

        // Keep contact object in sync with the resolved values
        com.springboot.POS.modal.StoreContact resolved = new com.springboot.POS.modal.StoreContact();
        resolved.setEmail(storeDTO.getEmail()   != null ? storeDTO.getEmail()   : "");
        resolved.setPhone(storeDTO.getPhone()   != null ? storeDTO.getPhone()   : "");
        resolved.setAddress(storeDTO.getStoreAddress() != null ? storeDTO.getStoreAddress() : "");
        storeDTO.setContact(resolved);

        storeDTO.setSubscriptionPlan(store.getSubscriptionPlan());
        storeDTO.setSubscriptionPurchaseDate(store.getSubscriptionPurchaseDate());
        storeDTO.setSubscriptionExpiry(store.getSubscriptionExpiry());
        storeDTO.setSubscriptionStatus(store.getSubscriptionStatus());
        storeDTO.setSubscriptionRenewalCount(store.getSubscriptionRenewalCount());
        storeDTO.setEstimatedBranches(store.getEstimatedBranches());
        storeDTO.setEstimatedUsers(store.getEstimatedUsers());
        storeDTO.setTotalRevenue(store.getTotalRevenue());
        storeDTO.setApprovedAt(store.getApprovedAt());
        storeDTO.setRegistrationRequestId(store.getRegistrationRequestId());
        return storeDTO;
    }

    public static Store toEntity(StoreDTO storeDTO, User storeAdmin){
        Store store = new Store();
        store.setId(storeDTO.getId());
        store.setBrand(storeDTO.getBrand());
        store.setDescription(storeDTO.getDescription());
        store.setStoreAdmin(storeAdmin);
        store.setStoreType(storeDTO.getStoreType());

        // Resolve address/email/phone: flat fields take priority, fall back to contact object
        String address = storeDTO.getStoreAddress();
        String email   = storeDTO.getEmail();
        String phone   = storeDTO.getPhone();
        if (address == null && storeDTO.getContact() != null) address = storeDTO.getContact().getAddress();
        if (email   == null && storeDTO.getContact() != null) email   = storeDTO.getContact().getEmail();
        if (phone   == null && storeDTO.getContact() != null) phone   = storeDTO.getContact().getPhone();

        // Write into both columns so they stay in sync
        store.setStoreAddress(address);
        store.setFullName(storeDTO.getFullName());
        com.springboot.POS.modal.StoreContact contact = new com.springboot.POS.modal.StoreContact();
        contact.setAddress(address != null ? address : "");
        contact.setEmail(email     != null ? email   : "");
        contact.setPhone(phone     != null ? phone   : "");
        store.setContact(contact);

        store.setSubscriptionPlan(storeDTO.getSubscriptionPlan());
        store.setEstimatedBranches(storeDTO.getEstimatedBranches());
        store.setEstimatedUsers(storeDTO.getEstimatedUsers());
        store.setTotalRevenue(storeDTO.getTotalRevenue());
        store.setApprovedAt(storeDTO.getApprovedAt());
        store.setRegistrationRequestId(storeDTO.getRegistrationRequestId());
        return store;
    }


}
