package com.springboot.POS.repository;

import com.springboot.POS.domain.StockMovementType;
import com.springboot.POS.modal.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("SELECT sm FROM StockMovement sm WHERE sm.branch.id = :branchId ORDER BY sm.createdAt DESC")
    List<StockMovement> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.branch.store.id = :storeId ORDER BY sm.createdAt DESC")
    List<StockMovement> findByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.product.id = :productId ORDER BY sm.createdAt DESC")
    List<StockMovement> findByProductId(@Param("productId") Long productId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.inventory.id = :inventoryId ORDER BY sm.createdAt DESC")
    List<StockMovement> findByInventoryId(@Param("inventoryId") Long inventoryId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.branch.id = :branchId AND sm.type = :type ORDER BY sm.createdAt DESC")
    List<StockMovement> findByBranchIdAndType(@Param("branchId") Long branchId, @Param("type") StockMovementType type);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.branch.id = :branchId AND sm.createdAt BETWEEN :startDate AND :endDate ORDER BY sm.createdAt DESC")
    List<StockMovement> findByBranchIdAndDateRange(@Param("branchId") Long branchId, 
                                                     @Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.branch.store.id = :storeId AND sm.createdAt BETWEEN :startDate AND :endDate ORDER BY sm.createdAt DESC")
    List<StockMovement> findByStoreIdAndDateRange(@Param("storeId") Long storeId, 
                                                    @Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);
}
