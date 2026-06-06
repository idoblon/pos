package com.springboot.POS.repository;

import com.springboot.POS.modal.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
    Optional<SubscriptionPayment> findByRegistrationRequestId(Long registrationRequestId);
    Optional<SubscriptionPayment> findByTransactionId(String transactionId);
    List<SubscriptionPayment> findByPaymentStatus(String paymentStatus);
    List<SubscriptionPayment> findByRegistrationRequestIdAndPaymentStatus(Long registrationRequestId, String paymentStatus);
}