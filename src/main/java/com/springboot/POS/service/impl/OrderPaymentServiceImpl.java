package com.springboot.POS.service.impl;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.modal.StorePaymentConfig;
import com.springboot.POS.repository.StorePaymentConfigRepository;
import com.springboot.POS.service.OrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private final StorePaymentConfigRepository paymentConfigRepository;
    private final RestTemplate restTemplate;

    @Value("${app.esewa.status-url}")
    private String esewaStatusUrl;

    @Value("${app.esewa.merchant-id}")
    private String defaultEsewaMerchantId;

    @Value("${app.khalti.base-url}")
    private String khaltiBaseUrl;

    @Value("${app.khalti.secret-key}")
    private String defaultKhaltiSecretKey;

    private static final String ESEWA_TEST_MERCHANT = "EPAYTEST";
    private static final String KHALTI_TEST_SECRET  = "test_secret_key_f59e8b7d18b4499ca40f68195a846e9b";

    @Value("${app.stripe.secret-key:}")
    private String stripeSecretKey;

    @Override
    public void verify(PaymentType type, String reference, Double amountReceived,
                       double total, Long storeId) throws Exception {
        switch (type) {
            case CASH   -> verifyCash(amountReceived, total);
            case ESEWA  -> verifyEsewa(reference, total, storeId);
            case KHALTI -> verifyKhalti(reference, total, storeId);
            case CARD   -> verifyCard(reference, total, storeId);
            default     -> throw new Exception("Unsupported payment method: " + type);
        }
    }

    @Override
    public void verifyCash(Double amountReceived, double total) throws Exception {
        if (amountReceived == null || !Double.isFinite(amountReceived) || amountReceived < total) {
            throw new Exception("Insufficient cash received.");
        }
    }

    @Override
    public void verifyEsewa(String transactionUuid, double amount, Long storeId) throws Exception {
        if (transactionUuid == null || !transactionUuid.matches("[A-Za-z0-9-]+")) {
            throw new Exception("A valid eSewa merchant transaction UUID is required.");
        }

        String merchantId = resolveEsewaMerchantId(storeId);
        if (merchantId == null || merchantId.isBlank()) {
            throw new Exception("eSewa is not configured for this store.");
        }

        // Skip live API call for test/UAT merchant — accept any valid UUID format
        if (ESEWA_TEST_MERCHANT.equalsIgnoreCase(merchantId)) {
            return;
        }

        String url = UriComponentsBuilder.fromHttpUrl(esewaStatusUrl)
                .queryParam("product_code", merchantId)
                .queryParam("total_amount", amount)
                .queryParam("transaction_uuid", transactionUuid)
                .toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();
            Object status = body == null ? null : body.get("status");
            Object providerAmount = body == null ? null : body.get("totalAmount");
            if (!response.getStatusCode().is2xxSuccessful()
                    || !"COMPLETE".equalsIgnoreCase(String.valueOf(status))
                    || !amountMatches(providerAmount, amount)) {
                throw new Exception("eSewa payment is not completed for this transaction.");
            }
        } catch (Exception ex) {
            throw new Exception("eSewa verification error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void verifyKhalti(String token, double amount, Long storeId) throws Exception {
        if (token == null || token.isBlank()) {
            throw new Exception("Khalti payment token is required.");
        }

        String secretKey = resolveKhaltiSecretKey(storeId);
        if (secretKey == null || secretKey.isBlank()) {
            throw new Exception("Khalti is not configured for this store.");
        }

        // Skip live API call for test secret key
        if (KHALTI_TEST_SECRET.equals(secretKey)) {
            return;
        }

        String url = UriComponentsBuilder.fromHttpUrl(khaltiBaseUrl + "/payment/status/")
                .queryParam("token", token)
                .queryParam("amount", Math.round(amount * 100))
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + secretKey);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<Void>(headers), Map.class);
            Map body = response.getBody();
            Object status = body == null ? null : body.get("status");
            Object state  = body == null ? null : body.get("state");
            if (!response.getStatusCode().is2xxSuccessful()
                    || !Boolean.TRUE.equals(status)
                    || !"COMPLETE".equalsIgnoreCase(String.valueOf(state))) {
                throw new Exception("Khalti payment is not completed.");
            }
        } catch (Exception ex) {
            throw new Exception("Khalti verification error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void verifyCard(String reference, double amount, Long storeId) throws Exception {
        if (reference == null || reference.isBlank()) {
            throw new Exception("A Stripe PaymentMethod ID is required to complete the payment.");
        }

        String secretKey = resolveStripeSecretKey(storeId);
        if (secretKey == null || secretKey.isBlank()) {
            throw new Exception("Stripe is not configured for this store.");
        }

        try {
            Stripe.apiKey = secretKey;
            long amountInCents = Math.round(amount * 100);

            // Use MANUAL confirmation: create intent, attach PM, then confirm
            // This avoids return_url requirement and works for server-side POS
            PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setPaymentMethod(reference)
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                            .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(createParams);
            String status = intent.getStatus();

            if ("succeeded".equals(status) || "requires_capture".equals(status)) {
                return;
            }
            if ("requires_action".equals(status) || "requires_source_action".equals(status)) {
                throw new Exception("Card requires additional authentication (3D Secure). Use a card that does not require 3DS for POS payments.");
            }
            throw new Exception("Stripe payment did not succeed. Status: " + status);
        } catch (com.stripe.exception.CardException ex) {
            throw new Exception("Card declined: " + ex.getCode() + " - " + ex.getMessage(), ex);
        } catch (com.stripe.exception.InvalidRequestException ex) {
            throw new Exception("Invalid Stripe request: " + ex.getMessage(), ex);
        } catch (com.stripe.exception.StripeException ex) {
            throw new Exception("Stripe error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isPaymentMethodEnabled(Long storeId, PaymentType type) {
        if (type == PaymentType.CASH) {
            return true;
        }
        // Check store-level config first
        var config = paymentConfigRepository.findFirstByStoreIdAndPaymentType(storeId, type);
        if (config.isPresent()) {
            return Boolean.TRUE.equals(config.get().getIsEnabled());
        }
        // Fall back to global config for CARD
        if (type == PaymentType.CARD) {
            return stripeSecretKey != null && !stripeSecretKey.isBlank();
        }
        // Fall back to global config for ESEWA
        if (type == PaymentType.ESEWA) {
            return defaultEsewaMerchantId != null && !defaultEsewaMerchantId.isBlank();
        }
        // Fall back to global config for KHALTI
        if (type == PaymentType.KHALTI) {
            return defaultKhaltiSecretKey != null && !defaultKhaltiSecretKey.isBlank();
        }
        return false;
    }

    private String resolveStripeSecretKey(Long storeId) {
        return paymentConfigRepository
                .findFirstByStoreIdAndPaymentType(storeId, PaymentType.CARD)
                .map(StorePaymentConfig::getCardSecretKey)
                .filter(s -> s != null && !s.isBlank())
                .orElse(stripeSecretKey);
    }

    private String resolveEsewaMerchantId(Long storeId) {
        return paymentConfigRepository
                .findFirstByStoreIdAndPaymentType(storeId, PaymentType.ESEWA)
                .map(StorePaymentConfig::getEsewaSettlementId)
                .filter(s -> !s.isBlank())
                .orElse(defaultEsewaMerchantId);
    }

    private String resolveKhaltiSecretKey(Long storeId) {
        return paymentConfigRepository
                .findFirstByStoreIdAndPaymentType(storeId, PaymentType.KHALTI)
                .map(StorePaymentConfig::getKhaltiSecretKey)
                .filter(s -> !s.isBlank())
                .orElse(defaultKhaltiSecretKey);
    }

    private boolean amountMatches(Object providerAmount, double expectedAmount) {
        return providerAmount instanceof Number number
                && Math.abs(number.doubleValue() - expectedAmount) < 0.005;
    }
}
