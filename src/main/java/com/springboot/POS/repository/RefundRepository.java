package com.springboot.POS.repository;

import com.springboot.POS.modal.Refund;
import com.springboot.POS.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByCashierIdAndCreatedAtBetween(
            Long cashier,
            LocalDateTime from,
            LocalDateTime to
    );
    List<Refund> findByCashierId(Long id);
    List<Refund> findByShiftReportId(Long id);
    List<Refund> findByBranchId(Long id);
    List<Refund> findByOrderId(Long orderId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Refund r WHERE r.branch.id = :branchId AND r.createdAt BETWEEN :from AND :to")
    List<Refund> findByBranchIdAndCreatedAtBetween(
            @org.springframework.data.repository.query.Param("branchId") Long branchId,
            @org.springframework.data.repository.query.Param("from") LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") LocalDateTime to
    );

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Refund r WHERE r.branch.store.id = :storeId")
    List<Refund> findByStoreId(@org.springframework.data.repository.query.Param("storeId") Long storeId);
}

