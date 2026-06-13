package com.springboot.POS.service;

import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StoreDTO;

import java.util.List;

public interface StoreService {

    StoreDTO createStore(StoreDTO storeDTO, User user);
    Store createStoreFromRegistration(String storeName, String description, String address, String phone, String storeType, String subscriptionPlan, Integer estimatedBranches, Integer estimatedUsers, String ownerName, String email);
    StoreDTO getStoreById(Long id) throws Exception;
    List<Store> getAllStores();
    List<StoreDTO> getAllStoreDTOs();
    Store getStoreByAdmin() throws UserException;
    StoreDTO updateStore(Long id, StoreDTO storeDTO) throws Exception;
    void deleteStore(Long id) throws UserException;
    StoreDTO getStoreByEmployee() throws UserException;
    StoreDTO moderateStore(Long id, StoreStatus status) throws Exception;
    void updateSubscriptionPlan(Long storeId, String subscriptionPlan) throws Exception;

}
