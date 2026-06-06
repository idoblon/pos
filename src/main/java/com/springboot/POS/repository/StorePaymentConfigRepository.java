package com.springboot.POS.repository;

import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.modal.StorePaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorePaymentConfigRepository extends JpaRepository<StorePaymentConfig, Long> {
    List<StorePaymentConfig> findByStoreId(Long storeId);
    List<StorePaymentConfig> findByStoreIdAndIsEnabled(Long storeId, Boolean isEnabled);
    Optional<StorePaymentConfig> findByStoreIdAndPaymentType(Long storeId, PaymentType paymentType);
    boolean existsByStoreIdAndPaymentType(Long storeId, PaymentType paymentType);
}