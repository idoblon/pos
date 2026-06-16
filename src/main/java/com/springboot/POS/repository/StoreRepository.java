package com.springboot.POS.repository;

import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Store findByStoreAdminId(Long adminId);

    Optional<Store> findByStoreAdmin(User storeAdmin);

}
