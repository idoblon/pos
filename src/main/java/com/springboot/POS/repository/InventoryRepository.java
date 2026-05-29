package com.springboot.POS.repository;

import com.springboot.POS.modal.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
   List<Inventory> findByBranchId(Long branchId);
   List<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.branch.id = :branchId")
   Optional<Inventory> findByProductIdAndBranchIdWithLock(@Param("productId") Long productId,
                                                           @Param("branchId") Long branchId);

   @Query("SELECT i FROM Inventory i WHERE i.branch.store.id = :storeId")
   List<Inventory> findByStoreId(@Param("storeId") Long storeId);

   @Query("SELECT i FROM Inventory i WHERE i.branch.id = :branchId AND i.quantity <= :threshold")
   List<Inventory> findLowStockByBranch(@Param("branchId") Long branchId, @Param("threshold") Integer threshold);

   @Query("SELECT i FROM Inventory i WHERE i.branch.store.id = :storeId AND i.quantity <= :threshold")
   List<Inventory> findLowStockByStore(@Param("storeId") Long storeId, @Param("threshold") Integer threshold);
}
