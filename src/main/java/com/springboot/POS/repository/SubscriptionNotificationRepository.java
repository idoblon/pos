package com.springboot.POS.repository;

import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.SubscriptionNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubscriptionNotificationRepository extends JpaRepository<SubscriptionNotification, Long> {

    List<SubscriptionNotification> findByStoreOrderByCreatedAtDesc(Store store);

    List<SubscriptionNotification> findByStoreAndIsReadOrderByCreatedAtDesc(Store store, Boolean isRead);

    @Query("SELECT sn FROM SubscriptionNotification sn WHERE sn.expiresAt IS NULL OR sn.expiresAt > :now ORDER BY sn.createdAt DESC")
    List<SubscriptionNotification> findActiveNotifications(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(sn) FROM SubscriptionNotification sn WHERE sn.store = :store AND sn.isRead = false")
    Long countUnreadByStore(@Param("store") Store store);

    void deleteByStoreAndCreatedAtBefore(Store store, LocalDateTime cutoffDate);
}
