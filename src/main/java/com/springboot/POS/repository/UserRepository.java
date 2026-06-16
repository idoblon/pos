package com.springboot.POS.repository;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByStoreAndDeletedFalse(Store store);
    List<User> findByStore_IdAndDeletedFalse(Long storeId);
    List<User> findByBranch_IdAndDeletedFalse(Long branchId);
    
    List<User> findByRole(UserRole role);

}
