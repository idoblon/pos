package com.springboot.POS.service;

import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StoreDTO;

import java.util.List;

public interface StoreService {

    StoreDTO createStore(StoreDTO storeDTO, User user);
    StoreDTO getStoreById(Long id) throws Exception;
    List<Store> getAllStores();
    Store getStoreByAdmin() throws UserException;
    StoreDTO updateStore(Long id, StoreDTO storeDTO) throws Exception;
    void deleteStore(Long id) throws UserException;
    StoreDTO getStoreByEmployee() throws UserException;

    StoreDTO moderateStore(Long id, StoreStatus status) throws Exception;

}
