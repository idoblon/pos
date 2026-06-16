package com.springboot.POS.service;

import com.springboot.POS.modal.Store;
import com.springboot.POS.payload.dto.SubscriptionDTO;
import com.springboot.POS.payload.dto.SubscriptionNotificationDTO;
import com.springboot.POS.payload.dto.SubscriptionStatsDTO;

import java.util.List;
import java.util.Map;

public interface SubscriptionService {
    
    // Subscription Management
    SubscriptionDTO getStoreSubscription(Long storeId);
    
    SubscriptionDTO getCurrentSubscription(String jwt);
    
    SubscriptionDTO renewSubscription(Long storeId, String plan, Map<String, Object> paymentDetails);
    
    SubscriptionDTO updateSubscriptionPlan(Long storeId, String newPlan);
    
    SubscriptionDTO createSubscription(Long storeId, String plan, Map<String, Object> paymentDetails);
    
    void suspendSubscription(Long storeId, String reason);
    
    void reactivateSubscription(Long storeId);
    
    // Expiration Management
    List<SubscriptionDTO> getExpiringSubscriptions(Integer days);
    
    void updateSubscriptionStatuses();
    
    String calculateSubscriptionStatus(Store store);
    
    // Statistics
    SubscriptionStatsDTO getSubscriptionStats();
    
    // Notifications
    List<SubscriptionNotificationDTO> getSubscriptionNotifications(Long storeId);
    
    void markNotificationAsRead(Long notificationId);
    
    void generateExpirationNotifications();
}
