package com.springboot.POS.service.impl;

import com.springboot.POS.configuration.JwtProvider;
import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.UserMapper;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.StoreContact;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.UserDTO;
import com.springboot.POS.payload.response.AuthResponse;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.AdminAuditService;
import com.springboot.POS.service.EmailService;
import com.springboot.POS.service.TrialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialServiceImpl implements TrialService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AdminAuditService auditService;
    private final EmailService emailService;
    private final com.springboot.POS.service.SubscriptionLimitService limitService;

    @Override
    @Transactional
    public AuthResponse startTrial(UserDTO userDto) throws UserException {
        if (userDto.getFullName() == null || userDto.getFullName().isBlank()) {
            throw new UserException("Full name is required");
        }
        if (userDto.getRole() == null || !UserRole.ROLE_STORE_ADMIN.equals(userDto.getRole())) {
            throw new UserException("Only store admin accounts can start a free trial");
        }
        if (userDto.getPassword() == null || userDto.getPassword().length() < 8) {
            throw new UserException("Password must be at least 8 characters");
        }
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new UserException("Email id already registered! Please sign in instead.");
        }
        if (userDto.getStoreName() == null || userDto.getStoreName().isBlank()) {
            throw new UserException("Store name is required for a free trial");
        }
        // Trials run on BASIC (1 store per owner email); owners needing more
        // stores subscribe to Enterprise first.
        limitService.requireStoreCreationAllowed(userDto.getEmail(), "BASIC");

        LocalDateTime now = LocalDateTime.now();
        // NOTE: UserDTO.storeType carries the store category (RETAIL, ...), not the
        // plan. Trials always start on BASIC; the owner upgrades after converting.
        String trialPlan = "BASIC";

        Store store = new Store();
        store.setBrand(userDto.getStoreName());
        store.setDescription(userDto.getStoreDescription());
        store.setStoreType(userDto.getStoreType() != null && !userDto.getStoreType().isBlank()
                ? userDto.getStoreType() : "RETAIL");
        store.setStatus(StoreStatus.ACTIVE);
        store.setSubscriptionPlan(trialPlan);
        store.setSubscriptionPurchaseDate(now);
        store.setSubscriptionExpiry(now.plusDays(TRIAL_DAYS));
        store.setSubscriptionStatus("TRIAL");
        store.setTrialStatus("TRIAL");
        store.setTrialStartedAt(now);
        store.setTrialEndsAt(now.plusDays(TRIAL_DAYS));

        StoreContact contact = new StoreContact();
        contact.setEmail(userDto.getStoreEmail() != null && !userDto.getStoreEmail().isBlank()
                ? userDto.getStoreEmail() : userDto.getEmail());
        if (userDto.getStorePhone() != null && !userDto.getStorePhone().isBlank()) {
            contact.setPhone(userDto.getStorePhone());
        } else if (userDto.getPhone() != null && !userDto.getPhone().isBlank()) {
            contact.setPhone(userDto.getPhone());
        }
        if (userDto.getStoreAddress() != null && !userDto.getStoreAddress().isBlank()) {
            contact.setAddress(userDto.getStoreAddress());
        }
        store.setContact(contact);
        store = storeRepository.save(store);

        User newUser = new User();
        newUser.setEmail(userDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        newUser.setRole(UserRole.ROLE_STORE_ADMIN);
        newUser.setFullName(userDto.getFullName().trim());
        newUser.setPhone(userDto.getPhone());
        newUser.setStore(store);
        User savedUser = userRepository.save(newUser);

        store.setStoreAdmin(savedUser);
        storeRepository.save(store);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDto.getEmail(), userDto.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtProvider.generateToken(authentication);

        auditService.record(null, "TRIAL_STARTED", "store", store.getId(),
                "14-day free trial started (" + trialPlan + ")");

        // Welcome email with login credentials must never break the signup.
        try {
            emailService.sendTrialStartedEmail(savedUser.getEmail(), savedUser.getFullName(),
                    store.getBrand(), savedUser.getEmail(),
                    store.getTrialEndsAt() != null ? store.getTrialEndsAt().toLocalDate().toString() : "");
        } catch (Exception e) {
            log.warn("Trial welcome email failed for {}: {}", savedUser.getEmail(), e.getMessage());
        }

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Trial started. Welcome to POS Pro — " + TRIAL_DAYS + " days free.");
        authResponse.setUser(UserMapper.toDTO(savedUser));
        authResponse.setRole(savedUser.getRole());
        authResponse.setStoreId(savedUser.getStoreId());
        authResponse.setBranchId(savedUser.getBranchId());
        authResponse.setStoreName(store.getBrand());
        return authResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTrialStatus(User user) {
        Map<String, Object> out = new LinkedHashMap<>();
        Store store = resolveStore(user);
        if (store == null) {
            out.put("trialStatus", "NONE");
            out.put("isTrial", false);
            out.put("isTrialActive", false);
            out.put("daysRemaining", 0);
            return out;
        }
        boolean isTrial = "TRIAL".equals(store.getTrialStatus());
        long daysRemaining = 0;
        if (isTrial && store.getTrialEndsAt() != null) {
            daysRemaining = Math.max(0, ChronoUnit.DAYS.between(
                    LocalDateTime.now().toLocalDate(), store.getTrialEndsAt().toLocalDate()));
        }
        out.put("storeId", store.getId());
        out.put("trialStatus", store.getTrialStatus() != null ? store.getTrialStatus() : "NONE");
        out.put("isTrial", isTrial);
        out.put("isTrialActive", isTrial && (store.getTrialEndsAt() == null
                || !store.getTrialEndsAt().isBefore(LocalDateTime.now())));
        out.put("trialEndsAt", store.getTrialEndsAt() != null ? store.getTrialEndsAt().toString() : null);
        out.put("daysRemaining", daysRemaining);
        out.put("subscriptionPlan", store.getSubscriptionPlan());
        out.put("subscriptionStatus", store.getSubscriptionStatus());
        return out;
    }

    @Override
    @Transactional
    public void convertTrial(Long storeId, String plan) {
        if (storeId == null) return;
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null || !"TRIAL".equals(store.getTrialStatus())) return;
        LocalDateTime now = LocalDateTime.now();
        store.setTrialStatus("CONVERTED");
        store.setConvertedAt(now);
        if (plan != null && !plan.isBlank()) {
            store.setSubscriptionPlan(plan.toUpperCase());
        }
        store.setSubscriptionPurchaseDate(now);
        store.setSubscriptionExpiry(now.plusYears(1));
        store.setSubscriptionStatus("ACTIVE");
        if (store.getStatus() != StoreStatus.ACTIVE) {
            store.setStatus(StoreStatus.ACTIVE);
        }
        storeRepository.save(store);
        auditService.record(null, "TRIAL_CONVERTED", "store", storeId,
                "Trial converted to paid (" + store.getSubscriptionPlan() + ")");
        log.info("Trial store {} converted to paid plan {}", storeId, store.getSubscriptionPlan());
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 5 2 * * *") // Daily at 02:05, just after the subscription-status job
    public void expireDueTrials() {
        List<Store> trials = storeRepository.findByTrialStatus("TRIAL");
        LocalDateTime now = LocalDateTime.now();
        for (Store store : trials) {
            if (store.getTrialEndsAt() == null) continue;
            if (!store.getTrialEndsAt().isAfter(now)) {
                store.setTrialStatus("EXPIRED");
                store.setSubscriptionStatus("EXPIRED");
                store.setStatus(StoreStatus.SUSPENDED);
                storeRepository.save(store);
                auditService.record(null, "TRIAL_EXPIRED", "store", store.getId(),
                        "14-day free trial lapsed unpaid");
                sendTrialMail(store, "expired", 0);
                log.info("Trial expired for store {}", store.getId());
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), store.getTrialEndsAt().toLocalDate());
            if (daysLeft == 3 || daysLeft == 1) {
                sendTrialMail(store, "expiring", daysLeft);
            }
        }
    }

    private void sendTrialMail(Store store, String kind, long daysLeft) {
        try {
            if (store.getStoreAdmin() == null || store.getStoreAdmin().getEmail() == null) return;
            String email = store.getStoreAdmin().getEmail();
            String name = store.getStoreAdmin().getFullName() != null
                    ? store.getStoreAdmin().getFullName() : "Store Admin";
            if ("expired".equals(kind)) {
                emailService.sendTrialExpiredEmail(email, name, store.getBrand());
            } else {
                emailService.sendTrialExpiringEmail(email, name, store.getBrand(), daysLeft);
            }
        } catch (Exception e) {
            log.warn("Trial lifecycle email failed for store {}: {}", store.getId(), e.getMessage());
        }
    }

    private Store resolveStore(User user) {
        if (user == null) return null;
        try {
            if (user.getStore() != null && user.getStore().getId() != null) {
                return storeRepository.findById(user.getStore().getId()).orElse(null);
            }
            if (user.getBranch() != null && user.getBranch().getStore() != null
                    && user.getBranch().getStore().getId() != null) {
                return storeRepository.findById(user.getBranch().getStore().getId()).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Unable to resolve trial store for user: {}", e.getMessage());
        }
        return null;
    }
}
