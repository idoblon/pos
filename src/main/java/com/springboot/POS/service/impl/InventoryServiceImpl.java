package com.springboot.POS.service.impl;

import com.springboot.POS.mapper.InventoryMapper;
import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.Inventory;
import com.springboot.POS.modal.Product;
import com.springboot.POS.payload.dto.InventoryDTO;
import com.springboot.POS.repository.BranchRepository;
import com.springboot.POS.repository.InventoryRepository;
import com.springboot.POS.repository.ProductRepository;
import com.springboot.POS.service.InventoryService;
import com.springboot.POS.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementService stockMovementService;
    private final com.springboot.POS.repository.StoreRepository storeRepository;


    @Override
    public InventoryDTO createInventory(InventoryDTO inventoryDTO) throws Exception {
        System.out.println("\n=== CREATING INVENTORY ===");
        System.out.println("DTO: branchId=" + inventoryDTO.getBranchId() + ", storeId=" + inventoryDTO.getStoreId());
        System.out.println("DTO: productId=" + inventoryDTO.getProductId() + ", quantity=" + inventoryDTO.getQuantity());
        
        Product product = productRepository.findById(inventoryDTO.getProductId()).orElseThrow(
                () -> new Exception("product doesn't exist....")
        );

        Inventory inventory;
        
        // Check if this is warehouse inventory (branchId is null)
        if (inventoryDTO.getBranchId() == null) {
            System.out.println("Creating WAREHOUSE inventory...");
            // Warehouse inventory
            if (inventoryDTO.getStoreId() == null) {
                throw new Exception("Store ID is required for warehouse inventory");
            }
            
            com.springboot.POS.modal.Store store = storeRepository.findById(inventoryDTO.getStoreId())
                    .orElseThrow(() -> new Exception("Store does not exist"));
            
            inventory = Inventory.builder()
                    .branch(null)  // NULL = Warehouse
                    .store(store)
                    .product(product)
                    .quantity(inventoryDTO.getQuantity())
                    .unitPrice(inventoryDTO.getUnitPrice())
                    .build();
            System.out.println("Warehouse inventory built: " + inventory.getQuantity() + " units");
        } else {
            System.out.println("Creating BRANCH inventory for branchId: " + inventoryDTO.getBranchId());
            // Branch inventory
            Branch branch = branchRepository.findById(inventoryDTO.getBranchId()).orElseThrow(
                    ()-> new Exception("branch does not exist....")
            );
            
            inventory = Inventory.builder()
                    .branch(branch)
                    .store(null)  // Store reference is in branch
                    .product(product)
                    .quantity(inventoryDTO.getQuantity())
                    .unitPrice(inventoryDTO.getUnitPrice())
                    .build();
            System.out.println("Branch inventory built: " + inventory.getQuantity() + " units");
        }
        
        System.out.println("Before save - Quantity: " + inventory.getQuantity());
        Inventory savedInventory = inventoryRepository.save(inventory);
        System.out.println("After save - ID: " + savedInventory.getId() + ", Quantity: " + savedInventory.getQuantity());
        System.out.println("Returning DTO...");
        InventoryDTO result = InventoryMapper.toDTO(savedInventory);
        System.out.println("DTO returned with quantity: " + result.getQuantity());
        System.out.println("=== INVENTORY CREATED SUCCESSFULLY ===\n");
        return result;
    }

    @Override
    public InventoryDTO updateInventory(Long id, InventoryDTO inventoryDTO) throws Exception {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(
                () -> new Exception("inventory not found...."));
        inventory.setQuantity(inventoryDTO.getQuantity());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toDTO(updatedInventory);
    }

    @Override
    public InventoryDTO updateStock(Long id, Integer quantity) throws Exception {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(
                () -> new Exception("inventory not found...."));
        inventory.setQuantity(quantity);
        Inventory updatedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toDTO(updatedInventory);
    }

    @Override
    public void deleteInventory(Long id) throws Exception {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(
                () -> new Exception("inventory not found...."));
        inventoryRepository.delete(inventory);
    }

    @Override
    public InventoryDTO getInventoryById(Long id) throws Exception {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(
                () -> new Exception("inventory not found...."));
        return InventoryMapper.toDTO(inventory);
    }

    @Override
    public List<InventoryDTO> getInventoryByProductAndBranchId(Long productId, Long branchId) {
        List<Inventory> inventories = inventoryRepository.findByProductIdAndBranchId(productId,branchId);
        return inventories.stream()
                .map(InventoryMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryDTO> getAllInventoryByBranchId(Long branchId) {
        List<Inventory> inventories = inventoryRepository.findByBranchId(branchId);
        return inventories.stream().map(
                InventoryMapper::toDTO
        ).collect(Collectors.toList());
    }

    @Override
    public List<InventoryDTO> getAllInventoryByStoreId(Long storeId) {
        System.out.println("\n=== GET ALL INVENTORY BY STORE ===");
        System.out.println("Store ID: " + storeId);
        
        try {
            List<Inventory> inventories = inventoryRepository.findByStoreId(storeId);
            System.out.println("Query returned: " + inventories.size() + " items");
            
            if (inventories.isEmpty()) {
                System.out.println("WARNING: Query returned 0 items!");
                // Try alternate query to debug
                System.out.println("Trying findWarehouseInventoryByStoreId...");
                List<Inventory> warehouseInventories = inventoryRepository.findWarehouseInventoryByStoreId(storeId);
                System.out.println("Warehouse query returned: " + warehouseInventories.size() + " items");
            }
            
            inventories.forEach(inv -> {
                String branchInfo = (inv.getBranch() != null) ? "Branch: " + inv.getBranch().getId() : "Warehouse (NULL)";
                System.out.println("  - ID: " + inv.getId() + 
                                 ", Product: " + inv.getProduct().getName() +
                                 ", Qty: " + inv.getQuantity() +
                                 ", " + branchInfo);
            });
            
            List<InventoryDTO> dtos = inventories.stream()
                    .map(InventoryMapper::toDTO)
                    .collect(Collectors.toList());
            
            System.out.println("Converted to " + dtos.size() + " DTOs");
            System.out.println("=== GET ALL INVENTORY COMPLETED ===\n");
            
            return dtos;
        } catch (Exception e) {
            System.err.println("ERROR in getAllInventoryByStoreId: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    @Override
    @Transactional
    public void deductStock(Long productId, Long branchId, int quantity) throws Exception {
        Inventory inventory = inventoryRepository
                .findByProductIdAndBranchIdWithLock(productId, branchId)
                .orElseThrow(() -> new Exception("Product not found in branch inventory"));

        if (inventory.getQuantity() < quantity) {
            throw new Exception("Insufficient stock for product id=" + productId
                    + ": available=" + inventory.getQuantity()
                    + ", required=" + quantity);
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void addStock(Long productId, Long branchId, int quantity) throws Exception {
        Inventory inventory = inventoryRepository
                .findByProductIdAndBranchIdWithLock(productId, branchId)
                .orElseThrow(() -> new Exception("Product not found in branch inventory"));

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    public List<InventoryDTO> getLowStockItems(Long branchId, int threshold) {
        return inventoryRepository.findLowStockByBranch(branchId, threshold).stream()
                .sorted(Comparator.comparingInt(Inventory::getQuantity))
                .map(InventoryMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryDTO> getLowStockItemsByStore(Long storeId, int threshold) {
        return inventoryRepository.findLowStockByStore(storeId, threshold).stream()
                .sorted(Comparator.comparingInt(Inventory::getQuantity))
                .map(InventoryMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryDTO> getWarehouseInventoryByStoreId(Long storeId) {
        List<Inventory> inventories = inventoryRepository.findWarehouseInventoryByStoreId(storeId);
        return inventories.stream()
                .map(InventoryMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public InventoryDTO getWarehouseInventoryByProductAndStore(Long productId, Long storeId) throws Exception {
        Inventory inventory = inventoryRepository.findWarehouseInventoryByProductAndStore(productId, storeId)
                .orElseThrow(() -> new Exception("Product not found in warehouse inventory"));
        return InventoryMapper.toDTO(inventory);
    }
}
