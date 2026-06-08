package com.springboot.POS.service;

import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.modal.Branch;

/**
 * Handles per-order payment verification for ESEWA, KHALTI, and CARD.
 * CASH needs no verification — just validates amount-received >= total.
 */
public interface OrderPaymentService {

    /** Verify eSewa payment reference against eSewa verify API. */
    void verifyEsewa(String reference, double amount, Long storeId) throws Exception;

    /** Verify Khalti token against Khalti lookup API. */
    void verifyKhalti(String token, double amount, Long storeId) throws Exception;

    /**
     * Validate a card transaction reference (last-4 digits stored for audit).
     * Real card charging happens client-side via a processor like Stripe;
     * here we just make sure the reference is present and the method is enabled.
     */
    void verifyCard(String reference, double amount, Long storeId) throws Exception;

    /** Validate cash: amount received must cover total. */
    void verifyCash(Double amountReceived, double total) throws Exception;

    /** Dispatch to the right verifier based on payment type. */
    void verify(PaymentType type, String reference, Double amountReceived,
                double total, Long storeId) throws Exception;

    /** Returns true when the store has this payment method enabled. */
    boolean isPaymentMethodEnabled(Long storeId, PaymentType type);
}
