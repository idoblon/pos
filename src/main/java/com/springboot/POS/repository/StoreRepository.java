package com.springboot.POS.repository;

import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Store findByStoreAdminId(Long adminId);

    Optional<Store> findByStoreAdmin(User storeAdmin);

    List<Store> findByTrialStatus(String trialStatus);

    /**
     * Live stores owned via the contact email. Backs the per-owner store cap
     * (BASIC/PROFESSIONAL allow 1, ENTERPRISE unlimited).
     */
    @Query("SELECT COUNT(s) FROM Store s WHERE LOWER(s.contact.email) = LOWER(:email) "
            + "AND (s.deleted = false OR s.deleted IS NULL)")
    long countByContactEmailIgnoreCase(@Param("email") String email);

}
