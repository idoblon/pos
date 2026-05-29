package com.springboot.POS.repository;

import com.springboot.POS.domain.RestockStatus;
import com.springboot.POS.modal.RestockRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestockRequestRepository extends JpaRepository<RestockRequest, Long> {

    @Query("SELECT r FROM RestockRequest r WHERE r.branch.store.id = :storeId ORDER BY r.createdAt DESC")
    List<RestockRequest> findByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT r FROM RestockRequest r WHERE r.branch.id = :branchId ORDER BY r.createdAt DESC")
    List<RestockRequest> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT r FROM RestockRequest r WHERE r.branch.store.id = :storeId AND r.status = :status ORDER BY r.createdAt DESC")
    List<RestockRequest> findByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") RestockStatus status);

    @Query("SELECT r FROM RestockRequest r WHERE r.branch.id = :branchId AND r.status = :status ORDER BY r.createdAt DESC")
    List<RestockRequest> findByBranchIdAndStatus(@Param("branchId") Long branchId, @Param("status") RestockStatus status);
}
