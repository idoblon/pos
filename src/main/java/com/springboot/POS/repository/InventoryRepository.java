package com.springboot.POS.repository;

import com.springboot.POS.modal.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
   List<Inventory> findByBranchId(Long branchId);
   List<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);
   Optional<Inventory> findFirstByProductIdAndBranchId(Long productId, Long branchId);
}
