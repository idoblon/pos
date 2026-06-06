package com.springboot.POS.service;

import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.modal.StorePaymentConfig;
import com.springboot.POS.payload.dto.StorePaymentConfigDTO;

import java.util.List;

public interface StorePaymentConfigService {
    
    // Configure payment method for store
    StorePaymentConfig configurePaymentMethod(Long storeId, StorePaymentConfigDTO configDTO);
    
    // Get all payment configurations for a store
    List<StorePaymentConfig> getStorePaymentConfigs(Long storeId);
    
    // Get enabled payment methods for a store
    List<StorePaymentConfig> getEnabledPaymentMethods(Long storeId);
    
    // Update payment configuration
    StorePaymentConfig updatePaymentConfig(Long configId, StorePaymentConfigDTO configDTO);
    
    // Enable/Disable payment method
    void togglePaymentMethod(Long configId, Boolean isEnabled);
    
    // Delete payment configuration
    void deletePaymentConfig(Long configId);
    
    // Check if payment method is available for store
    boolean isPaymentMethodAvailable(Long storeId, PaymentType paymentType);
    
    // Initialize default payment methods for new store
    void initializeDefaultPaymentMethods(Long storeId);
}