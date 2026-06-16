package com.springboot.POS.service.impl;

import com.springboot.POS.configuration.JwtProvider;
import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.SubscriptionNotification;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.SubscriptionDTO;
import com.springboot.POS.payload.dto.SubscriptionNotificationDTO;
import com.springboot.POS.payload.dto.SubscriptionStatsDTO;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.repository.SubscriptionNotificationRepository;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final StoreRepository storeRepository;
    private final SubscriptionNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    private static final Map<String, Double> SUBSCRIPTION_PRICES = Map.of(
        "BASIC", 3500.0,
        "PROFESSIONAL", 7000.0,
        "ENTERPRISE", 10000.0
    );

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDTO getStoreSubscription(Long storeId) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));
        return mapToSubscriptionDTO(store);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDTO getCurrentSubscription(String jwt) {
        String email = jwtProvider.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Store store = storeRepository.findByStoreAdmin(user)
            .orElseThrow(() -> new RuntimeException("Store not found for this user"));
        
        return mapToSubscriptionDTO(store);
    }

    @Override
    @Transactional
    public SubscriptionDTO renewSubscription(Long storeId, String plan, Map<String, Object> paymentDetails) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiry = now.plusYears(1);

        store.setSubscriptionPlan(plan != null ? plan : store.getSubscriptionPlan());
        store.setSubscriptionPurchaseDate(now);
        store.setSubscriptionExpiry(newExpiry);
        store.setSubscriptionStatus("ACTIVE");
        store.setLastSubscriptionRenewal(now);
        store.setSubscriptionRenewalCount(
            (store.getSubscriptionRenewalCount() != null ? store.getSubscriptionRenewalCount() : 0) + 1
        );

        if (store.getStatus() == StoreStatus.SUSPENDED) {
            store.setStatus(StoreStatus.ACTIVE);
        }

        Store savedStore = storeRepository.save(store);

        // Clear old notifications
        notificationRepository.deleteByStoreAndCreatedAtBefore(store, now.minusDays(30));

        return mapToSubscriptionDTO(savedStore);
    }

    @Override
    @Transactional
    public SubscriptionDTO updateSubscriptionPlan(Long storeId, String newPlan) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

        if (!SUBSCRIPTION_PRICES.containsKey(newPlan)) {
            throw new RuntimeException("Invalid subscription plan: " + newPlan);
        }

        store.setSubscriptionPlan(newPlan);
        Store savedStore = storeRepository.save(store);

        return mapToSubscriptionDTO(savedStore);
    }

    @Override
    @Transactional
    public SubscriptionDTO createSubscription(Long storeId, String plan, Map<String, Object> paymentDetails) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusYears(1);

        store.setSubscriptionPlan(plan != null ? plan : "BASIC");
        store.setSubscriptionPurchaseDate(now);
        store.setSubscriptionExpiry(expiry);
        store.setSubscriptionStatus("ACTIVE");
        store.setSubscriptionRenewalCount(0);
        store.setStatus(StoreStatus.ACTIVE);

        Store savedStore = storeRepository.save(store);

        return mapToSubscriptionDTO(savedStore);
    }

    @Override
    @Transactional
    public void suspendSubscription(Long storeId, String reason) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

        store.setSubscriptionStatus("SUSPENDED");
        store.setStatus(StoreStatus.SUSPENDED);
        storeRepository.save(store);

        // Create suspension notification
        SubscriptionNotification notification = SubscriptionNotification.builder()
            .store(store)
            .type("EXPIRED")
            .title("Subscription Suspended")
            .message("Your subscription has been suspended. Reason: " + reason)
            .priority("HIGH")
            .isRead(false)
            .build();

        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void reactivateSubscription(Long storeId) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

        String newStatus = calculateSubscriptionStatus(store);
        store.setSubscriptionStatus(newStatus);

        if (newStatus.equals("ACTIVE")) {
            store.setStatus(StoreStatus.ACTIVE);
        }

        storeRepository.save(store);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionDTO> getExpiringSubscriptions(Integer days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(days != null ? days : 60);

        return storeRepository.findAll().stream()
            .filter(store -> store.getSubscriptionExpiry() != null)
            .filter(store -> {
                LocalDateTime expiry = store.getSubscriptionExpiry();
                return expiry.isAfter(now) && expiry.isBefore(futureDate);
            })
            .map(this::mapToSubscriptionDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void updateSubscriptionStatuses() {
        List<Store> stores = storeRepository.findAll();

        for (Store store : stores) {
            if (store.getSubscriptionExpiry() != null && !store.getSubscriptionStatus().equals("SUSPENDED")) {
                String newStatus = calculateSubscriptionStatus(store);
                if (!newStatus.equals(store.getSubscriptionStatus())) {
                    store.setSubscriptionStatus(newStatus);

                    if (newStatus.equals("EXPIRED")) {
                        store.setStatus(StoreStatus.SUSPENDED);
                    }

                    storeRepository.save(store);
                }
            }
        }
    }

    @Override
    public String calculateSubscriptionStatus(Store store) {
        if (store.getSubscriptionExpiry() == null) {
            return "ACTIVE";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = store.getSubscriptionExpiry();

        if (expiry.isBefore(now)) {
            return "EXPIRED";
        }

        long daysRemaining = ChronoUnit.DAYS.between(now, expiry);

        if (daysRemaining <= 30) {
            return "EXPIRING_SOON";
        }

        return "ACTIVE";
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatsDTO getSubscriptionStats() {
        List<Store> stores = storeRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long activeCount = stores.stream()
            .filter(s -> "ACTIVE".equals(s.getSubscriptionStatus()))
            .count();

        long expiringCount = stores.stream()
            .filter(s -> s.getSubscriptionExpiry() != null)
            .filter(s -> {
                LocalDateTime expiry = s.getSubscriptionExpiry();
                long days = ChronoUnit.DAYS.between(now, expiry);
                return days > 0 && days <= 60;
            })
            .count();

        long expiredCount = stores.stream()
            .filter(s -> "EXPIRED".equals(s.getSubscriptionStatus()))
            .count();

        long suspendedCount = stores.stream()
            .filter(s -> "SUSPENDED".equals(s.getSubscriptionStatus()))
            .count();

        long basicCount = stores.stream()
            .filter(s -> "BASIC".equals(s.getSubscriptionPlan()))
            .count();

        long professionalCount = stores.stream()
            .filter(s -> "PROFESSIONAL".equals(s.getSubscriptionPlan()))
            .count();

        long enterpriseCount = stores.stream()
            .filter(s -> "ENTERPRISE".equals(s.getSubscriptionPlan()))
            .count();

        double totalRevenue = stores.stream()
            .filter(s -> s.getSubscriptionPlan() != null)
            .mapToDouble(s -> SUBSCRIPTION_PRICES.getOrDefault(s.getSubscriptionPlan(), 0.0))
            .sum();

        return SubscriptionStatsDTO.builder()
            .totalStores((long) stores.size())
            .activeSubscriptions(activeCount)
            .expiringCount(expiringCount)
            .expiredCount(expiredCount)
            .suspendedCount(suspendedCount)
            .totalRevenue(totalRevenue)
            .basicCount(basicCount)
            .professionalCount(professionalCount)
            .enterpriseCount(enterpriseCount)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionNotificationDTO> getSubscriptionNotifications(Long storeId) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

        return notificationRepository.findByStoreOrderByCreatedAtDesc(store).stream()
            .map(this::mapToNotificationDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Long notificationId) {
        SubscriptionNotification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 8 * * *") // Run daily at 8 AM
    public void generateExpirationNotifications() {
        List<Store> stores = storeRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Store store : stores) {
            if (store.getSubscriptionExpiry() == null) continue;

            long daysRemaining = ChronoUnit.DAYS.between(now, store.getSubscriptionExpiry());

            // Generate notification for critical periods
            if (daysRemaining == 60 || daysRemaining == 30 || daysRemaining == 7 || daysRemaining == 0) {
                String type = daysRemaining <= 0 ? "EXPIRED" :
                             daysRemaining <= 7 ? "CRITICAL" : "WARNING";

                String title = daysRemaining <= 0 ? "Subscription Expired" :
                              "Subscription Expiring Soon";

                String message = daysRemaining <= 0 ?
                    "Your subscription has expired. Renew now to continue using the POS system." :
                    String.format("Your subscription expires in %d day%s. Renew now to avoid service interruption.",
                        daysRemaining, daysRemaining == 1 ? "" : "s");

                String priority = daysRemaining <= 7 ? "HIGH" : "MEDIUM";

                // Check if notification already exists for this period
                List<SubscriptionNotification> existingNotifications =
                    notificationRepository.findByStoreAndIsReadOrderByCreatedAtDesc(store, false);

                boolean notificationExists = existingNotifications.stream()
                    .anyMatch(n -> n.getDaysRemaining() != null &&
                                  n.getDaysRemaining().equals((int) daysRemaining));

                if (!notificationExists) {
                    SubscriptionNotification notification = SubscriptionNotification.builder()
                        .store(store)
                        .type(type)
                        .title(title)
                        .message(message)
                        .priority(priority)
                        .isRead(false)
                        .daysRemaining((int) daysRemaining)
                        .build();

                    notificationRepository.save(notification);
                }
            }
        }
    }

    private SubscriptionDTO mapToSubscriptionDTO(Store store) {
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = store.getSubscriptionExpiry() != null ?
            ChronoUnit.DAYS.between(now, store.getSubscriptionExpiry()) : 0;

        Double annualPrice = SUBSCRIPTION_PRICES.getOrDefault(
            store.getSubscriptionPlan(), SUBSCRIPTION_PRICES.get("BASIC")
        );

        return SubscriptionDTO.builder()
            .storeId(store.getId())
            .storeName(store.getBrand())
            .subscriptionPlan(store.getSubscriptionPlan())
            .subscriptionPurchaseDate(store.getSubscriptionPurchaseDate())
            .subscriptionExpiry(store.getSubscriptionExpiry())
            .subscriptionStatus(store.getSubscriptionStatus())
            .daysRemaining((int) daysRemaining)
            .subscriptionRenewalCount(store.getSubscriptionRenewalCount())
            .lastSubscriptionRenewal(store.getLastSubscriptionRenewal())
            .annualPrice(annualPrice)
            .monthlyPrice(annualPrice / 12)
            .build();
    }

    private SubscriptionNotificationDTO mapToNotificationDTO(SubscriptionNotification notification) {
        return SubscriptionNotificationDTO.builder()
            .id(notification.getId())
            .storeId(notification.getStore().getId())
            .storeName(notification.getStore().getBrand())
            .type(notification.getType())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .priority(notification.getPriority())
            .isRead(notification.getIsRead())
            .createdAt(notification.getCreatedAt())
            .expiresAt(notification.getExpiresAt())
            .daysRemaining(notification.getDaysRemaining())
            .build();
    }
}
