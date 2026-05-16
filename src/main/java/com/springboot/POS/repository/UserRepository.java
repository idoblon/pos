package com.springboot.POS.repository;

import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    List<User> findByStoreAndDeletedFalse(Store store);
    List<User> findByBranchIdAndDeletedFalse(Long branchId);

}
