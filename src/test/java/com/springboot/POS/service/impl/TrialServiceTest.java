package com.springboot.POS.service.impl;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Store;
import com.springboot.POS.payload.dto.UserDTO;
import com.springboot.POS.payload.response.AuthResponse;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.service.AuthService;
import com.springboot.POS.service.TrialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live-DB verification for the free-trial loop (runs only with
 * {@code -Dtrial.live=true}; needs a scratch MySQL database because the
 * default H2 test profile cannot create the reserved-word {@code user}
 * table). Everything rolls back — no rows persist.
 *
 * <pre>
 *   CREATE DATABASE IF NOT EXISTS pos_test;
 *   $env:DB_USERNAME="..."; $env:DB_PASSWORD="..."
 *   .\mvnw.cmd test -Dtest=TrialServiceTest -Dtrial.live=true
 * </pre>
 *
 * Proves: trial signup creates an instantly usable login (same password),
 * trial status is reported, conversion works, and a lapsed trial can no
 * longer log in.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=${TRIAL_TEST_DB_URL:jdbc:mysql://localhost:3306/pos_test}",
        "spring.datasource.username=${DB_USERNAME:root}",
        "spring.datasource.password=${DB_PASSWORD:}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.flyway.enabled=false",
})
@EnabledIfSystemProperty(named = "trial.live", matches = "true")
@Transactional
class TrialServiceTest {

    @Autowired
    private TrialService trialService;

    @Autowired
    private AuthService authService;

    @Autowired
    private StoreRepository storeRepository;

    private UserDTO trialForm(String email) {
        UserDTO dto = new UserDTO();
        dto.setFullName("Trial Owner");
        dto.setEmail(email);
        dto.setPhone("9800000001");
        dto.setPassword("trialpass123");
        dto.setRole(UserRole.ROLE_STORE_ADMIN);
        dto.setStoreName("Trial Store " + UUID.randomUUID().toString().substring(0, 6));
        dto.setStoreType("RETAIL");
        dto.setStoreAddress("Kathmandu");
        return dto;
    }

    @Test
    void trialSignupCreatesInstantLoginWithSamePassword() throws Exception {
        String email = "trial-" + UUID.randomUUID() + "@example.com";
        String password = "trialpass123";

        AuthResponse signup = trialService.startTrial(trialForm(email));
        assertNotNull(signup.getJwt());
        assertNotNull(signup.getStoreId());

        Store store = storeRepository.findById(signup.getStoreId()).orElseThrow();
        assertEquals("TRIAL", store.getTrialStatus());
        assertNotNull(store.getTrialEndsAt());

        // Login with the password set during the trial form fill-up.
        UserDTO login = new UserDTO();
        login.setEmail(email);
        login.setPassword(password);
        AuthResponse loggedIn = authService.login(login);
        assertNotNull(loggedIn.getJwt());
        assertEquals(signup.getStoreId(), loggedIn.getStoreId());

        // Trial status endpoint data.
        Map<String, Object> status = trialService.getTrialStatus(
                store.getStoreAdmin());
        assertEquals("TRIAL", status.get("trialStatus"));
        assertEquals(Boolean.TRUE, status.get("isTrialActive"));
    }

    @Test
    void expiredTrialCannotLoginAndJobLapsesIt() throws Exception {
        String email = "expired-" + UUID.randomUUID() + "@example.com";
        AuthResponse signup = trialService.startTrial(trialForm(email));

        Store store = storeRepository.findById(signup.getStoreId()).orElseThrow();
        store.setTrialEndsAt(LocalDateTime.now().minusDays(1));
        storeRepository.save(store);

        trialService.expireDueTrials();

        Store lapsed = storeRepository.findById(signup.getStoreId()).orElseThrow();
        assertEquals("EXPIRED", lapsed.getTrialStatus());

        UserDTO login = new UserDTO();
        login.setEmail(email);
        login.setPassword("trialpass123");
        UserException ex = assertThrows(UserException.class, () -> authService.login(login));
        assertTrue(ex.getMessage().toLowerCase().contains("trial"));
    }

    @Test
    void approvingPaymentConvertsTrial() throws Exception {
        String email = "convert-" + UUID.randomUUID() + "@example.com";
        AuthResponse signup = trialService.startTrial(trialForm(email));

        trialService.convertTrial(signup.getStoreId(), "PROFESSIONAL");

        Store store = storeRepository.findById(signup.getStoreId()).orElseThrow();
        assertEquals("CONVERTED", store.getTrialStatus());
        assertEquals("PROFESSIONAL", store.getSubscriptionPlan());
        assertEquals("ACTIVE", store.getSubscriptionStatus());
        assertNotNull(store.getConvertedAt());
        assertTrue(store.getSubscriptionExpiry().isAfter(LocalDateTime.now().plusMonths(11)));
    }
}
