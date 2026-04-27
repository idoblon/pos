package com.springboot.POS.repository;

import com.springboot.POS.modal.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Inventory findByProductIdAndBranchId(Long productId, Long BranchId);
    List<Inventory> findByBranchId(Long branchId);
}
