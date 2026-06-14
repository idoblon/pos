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

   // Warehouse inventory queries (branchId is null)
   @Query(value = "SELECT i.* FROM inventory i WHERE i.store_id = :storeId AND i.branch_id IS NULL", nativeQuery = true)
   List<Inventory> findWarehouseInventoryByStoreId(@Param("storeId") Long storeId);

   @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.store.id = :storeId AND i.branch IS NULL")
   Optional<Inventory> findWarehouseInventoryByProductAndStore(@Param("productId") Long productId, @Param("storeId") Long storeId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.branch.id = :branchId")
   Optional<Inventory> findByProductIdAndBranchIdWithLock(@Param("productId") Long productId,
                                                           @Param("branchId") Long branchId);

   // Get all inventory (warehouse + branches) for a store
   @Query(value = "SELECT i.* FROM inventory i WHERE i.store_id = :storeId OR i.branch_id IN (SELECT id FROM branch WHERE store_id = :storeId)", nativeQuery = true)
   List<Inventory> findByStoreId(@Param("storeId") Long storeId);

   @Query("SELECT i FROM Inventory i WHERE (i.branch IS NOT NULL AND i.branch.store.id = :storeId) AND i.quantity <= :threshold")
   List<Inventory> findLowStockByBranch(@Param("storeId") Long storeId, @Param("threshold") Integer threshold);

   @Query("SELECT i FROM Inventory i WHERE (i.store.id = :storeId OR (i.branch IS NOT NULL AND i.branch.store.id = :storeId)) AND i.quantity <= :threshold")
   List<Inventory> findLowStockByStore(@Param("storeId") Long storeId, @Param("threshold") Integer threshold);
}
