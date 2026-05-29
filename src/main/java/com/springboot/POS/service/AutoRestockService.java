package com.springboot.POS.service;

public interface AutoRestockService {
    
    void checkAndCreateAutoRequests(Long branchId) throws Exception;
    
    void checkAndCreateAutoRequestsForStore(Long storeId) throws Exception;
    
    void checkInventoryAfterSale(Long inventoryId) throws Exception;
}
