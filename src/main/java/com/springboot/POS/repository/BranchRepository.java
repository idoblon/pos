package com.springboot.POS.repository;

import com.springboot.POS.modal.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByStoreId(Long storeId);
    
    @Query("SELECT b FROM Branch b WHERE b.store.id = :storeId AND (b.deleted = false OR b.deleted IS NULL)")
    List<Branch> findByStoreIdAndDeletedFalse(@Param("storeId") Long storeId);
}
