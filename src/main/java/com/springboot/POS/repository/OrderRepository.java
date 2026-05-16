package com.springboot.POS.repository;

import com.springboot.POS.modal.Order;
import com.springboot.POS.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);
    List<Order> findByBranchId(Long branchId);
    List<Order> findByCashierId(Long cashierId);
    List<Order> findByBranchIdAndCreatedAtBetween(Long branchId, LocalDateTime from, LocalDateTime to);
    List<Order> findByCashierAndCreatedAtBetween(User cashier, LocalDateTime from, LocalDateTime to);
    List<Order> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId AND o.branch.store.id = :storeId")
    List<Order> findByBranchIdAndStoreId(@Param("branchId") Long branchId, @Param("storeId") Long storeId);

    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.id = :orderId AND o.branch.store.id = :storeId")
    boolean existsByIdAndStoreId(@Param("orderId") Long orderId, @Param("storeId") Long storeId);

    @Query("SELECT o FROM Order o WHERE o.branch.store.id = :storeId")
    List<Order> findByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT o FROM Order o WHERE o.branch.store.id = :storeId AND o.createdAt BETWEEN :from AND :to")
    List<Order> findByStoreIdAndCreatedAtBetween(@Param("storeId") Long storeId,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId AND o.createdAt BETWEEN :from AND :to")
    List<Order> findByBranchIdAndDateRange(@Param("branchId") Long branchId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);
}
