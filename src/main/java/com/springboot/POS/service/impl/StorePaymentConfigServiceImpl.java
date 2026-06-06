package com.springboot.POS.service.impl;

import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.StorePaymentConfig;
import com.springboot.POS.payload.dto.StorePaymentConfigDTO;
import com.springboot.POS.repository.StorePaymentConfigRepository;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.service.StorePaymentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StorePaymentConfigServiceImpl implements StorePaymentConfigService {

    private final StorePaymentConfigRepository paymentConfigRepository;
    private final StoreRepository storeRepository;

    @Override
    public StorePaymentConfig configurePaymentMethod(Long storeId, StorePaymentConfigDTO configDTO) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        // Check if configuration already exists
        if (paymentConfigRepository.existsByStoreIdAndPaymentType(storeId, configDTO.getPaymentType())) {
            throw new RuntimeException("Payment method already configured for this store");
        }

        StorePaymentConfig config = new StorePaymentConfig();
        config.setStore(store);
        config.setPaymentType(configDTO.getPaymentType());
        config.setIsEnabled(configDTO.getIsEnabled() != null ? configDTO.getIsEnabled() : true);
        
        // Set payment gateway credentials
        updateConfigFields(config, configDTO);

        return paymentConfigRepository.save(config);
    }

    @Override
    public List<StorePaymentConfig> getStorePaymentConfigs(Long storeId) {
        return paymentConfigRepository.findByStoreId(storeId);
    }

    @Override
    public List<StorePaymentConfig> getEnabledPaymentMethods(Long storeId) {
        return paymentConfigRepository.findByStoreIdAndIsEnabled(storeId, true);
    }

    @Override
    public StorePaymentConfig updatePaymentConfig(Long configId, StorePaymentConfigDTO configDTO) {
        StorePaymentConfig config = paymentConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Payment configuration not found"));

        updateConfigFields(config, configDTO);

        return paymentConfigRepository.save(config);
    }

    @Override
    public void togglePaymentMethod(Long configId, Boolean isEnabled) {
        StorePaymentConfig config = paymentConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Payment configuration not found"));

        config.setIsEnabled(isEnabled);
        paymentConfigRepository.save(config);
    }

    @Override
    public void deletePaymentConfig(Long configId) {
        paymentConfigRepository.deleteById(configId);
    }

    @Override
    public boolean isPaymentMethodAvailable(Long storeId, PaymentType paymentType) {
        return paymentConfigRepository.findByStoreIdAndPaymentType(storeId, paymentType)
                .map(StorePaymentConfig::getIsEnabled)
                .orElse(false);
    }

    @Override
    public void initializeDefaultPaymentMethods(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        // Initialize CASH payment (always enabled by default)
        if (!paymentConfigRepository.existsByStoreIdAndPaymentType(storeId, PaymentType.CASH)) {
            StorePaymentConfig cashConfig = new StorePaymentConfig();
            cashConfig.setStore(store);
            cashConfig.setPaymentType(PaymentType.CASH);
            cashConfig.setIsEnabled(true);
            cashConfig.setNotes("Cash payment - No additional configuration needed");
            paymentConfigRepository.save(cashConfig);
        }

        // Initialize other payment methods (disabled by default)
        PaymentType[] otherMethods = {PaymentType.ESEWA, PaymentType.KHALTI, PaymentType.CARD, PaymentType.BANK_TRANSFER};
        for (PaymentType paymentType : otherMethods) {
            if (!paymentConfigRepository.existsByStoreIdAndPaymentType(storeId, paymentType)) {
                StorePaymentConfig config = new StorePaymentConfig();
                config.setStore(store);
                config.setPaymentType(paymentType);
                config.setIsEnabled(false);
                config.setNotes("Configure payment gateway credentials to enable this method");
                paymentConfigRepository.save(config);
            }
        }
    }

    private void updateConfigFields(StorePaymentConfig config, StorePaymentConfigDTO configDTO) {
        if (configDTO.getIsEnabled() != null) {
            config.setIsEnabled(configDTO.getIsEnabled());
        }

        // eSewa
        if (configDTO.getEsewaSettlementId() != null) {
            config.setEsewaSettlementId(configDTO.getEsewaSettlementId());
        }
        if (configDTO.getEsewaSecretKey() != null) {
            config.setEsewaSecretKey(configDTO.getEsewaSecretKey());
        }

        // Khalti
        if (configDTO.getKhaltiPublicKey() != null) {
            config.setKhaltiPublicKey(configDTO.getKhaltiPublicKey());
        }
        if (configDTO.getKhaltiSecretKey() != null) {
            config.setKhaltiSecretKey(configDTO.getKhaltiSecretKey());
        }

        // Card
        if (configDTO.getCardProcessorName() != null) {
            config.setCardProcessorName(configDTO.getCardProcessorName());
        }
        if (configDTO.getCardApiKey() != null) {
            config.setCardApiKey(configDTO.getCardApiKey());
        }
        if (configDTO.getCardSecretKey() != null) {
            config.setCardSecretKey(configDTO.getCardSecretKey());
        }

        // Bank
        if (configDTO.getBankName() != null) {
            config.setBankName(configDTO.getBankName());
        }
        if (configDTO.getAccountNumber() != null) {
            config.setAccountNumber(configDTO.getAccountNumber());
        }
        if (configDTO.getAccountHolderName() != null) {
            config.setAccountHolderName(configDTO.getAccountHolderName());
        }
        if (configDTO.getIfscCode() != null) {
            config.setIfscCode(configDTO.getIfscCode());
        }

        if (configDTO.getNotes() != null) {
            config.setNotes(configDTO.getNotes());
        }
    }
}