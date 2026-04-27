package com.springboot.POS.repository;

import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    String email(String email);
    List<User> findByStore(Store store);
    List<User> findByBranchId(Long branchId);

}
