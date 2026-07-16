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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementService stockMovementService;
    private final com.springboot.POS.repository.StoreRepository storeRepository;


    @Override
    @Transactional
    public InventoryDTO createInventory(InventoryDTO inventoryDTO) throws Exception {
        validateNonNegativeQuantity(inventoryDTO.getQuantity());
        Product product = productRepository.findById(inventoryDTO.getProductId()).orElseThrow(
                () -> new Exception("product doesn't exist....")
        );

        Inventory inventory;
        
        // Check if this is warehouse inventory (branchId is null)
        if (inventoryDTO.getBranchId() == null) {
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
        } else {
            // Branch inventory
            Branch branch = branchRepository.findById(inventoryDTO.getBranchId()).orElseThrow(
                    ()-> new Exception("branch does not exist....")
            );
            
            inventoryRepository.upsertBranchInventory(
                    branch.getId(), product.getId(), inventoryDTO.getQuantity(), inventoryDTO.getUnitPrice());
            Inventory savedInventory = inventoryRepository
                    .findByProductIdAndBranchIdWithLock(product.getId(), branch.getId())
                    .orElseThrow(() -> new IllegalStateException("Unable to create branch inventory"));
            return InventoryMapper.toDTO(savedInventory);
        }
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        InventoryDTO result = InventoryMapper.toDTO(savedInventory);
        return result;
    }

    @Override
    @Transactional
    public InventoryDTO updateInventory(Long id, InventoryDTO inventoryDTO) throws Exception {
        validateNonNegativeQuantity(inventoryDTO.getQuantity());
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(
                () -> new Exception("inventory not found...."));
        inventory.setQuantity(inventoryDTO.getQuantity());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toDTO(updatedInventory);
    }

    @Override
    @Transactional
    public InventoryDTO updateStock(Long id, Integer quantity) throws Exception {
        validateNonNegativeQuantity(quantity);
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
        try {
            List<Inventory> inventories = inventoryRepository.findByStoreId(storeId);
            return inventories.stream()
                    .map(InventoryMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Unable to load inventory for storeId={}", storeId, e);
            throw new IllegalStateException("Unable to load store inventory", e);
        }
    }

    @Override
    @Transactional
    public void deductStock(Long productId, Long branchId, int quantity) throws Exception {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock deduction quantity must be greater than zero");
        }
        List<Inventory> rows = inventoryRepository.findAllByProductIdAndBranchIdWithLock(productId, branchId);
        if (rows.isEmpty()) {
            throw new Exception("Product not found in branch inventory");
        }

        // Auto-merge duplicates: keep the first row, sum all quantities, delete the rest
        Inventory primary = rows.get(0);
        if (rows.size() > 1) {
            int total = rows.stream().mapToInt(Inventory::getQuantity).sum();
            primary.setQuantity(total);
            inventoryRepository.save(primary);
            List<Inventory> duplicates = rows.subList(1, rows.size());
            inventoryRepository.deleteAll(duplicates);
            inventoryRepository.flush();
        }

        if (primary.getQuantity() < quantity) {
            throw new Exception("Insufficient stock for product id=" + productId
                    + ": available=" + primary.getQuantity()
                    + ", required=" + quantity);
        }

        primary.setQuantity(primary.getQuantity() - quantity);
        inventoryRepository.save(primary);
    }

    @Override
    @Transactional
    public void addStock(Long productId, Long branchId, int quantity) throws Exception {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock addition quantity must be greater than zero");
        }
        List<Inventory> rows = inventoryRepository.findAllByProductIdAndBranchIdWithLock(productId, branchId);
        if (rows.isEmpty()) {
            throw new Exception("Product not found in branch inventory");
        }
        Inventory primary = rows.get(0);
        if (rows.size() > 1) {
            int total = rows.stream().mapToInt(Inventory::getQuantity).sum();
            primary.setQuantity(total);
            inventoryRepository.deleteAll(rows.subList(1, rows.size()));
            inventoryRepository.flush();
        }
        primary.setQuantity(primary.getQuantity() + quantity);
        inventoryRepository.save(primary);
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

    private void validateNonNegativeQuantity(Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Inventory quantity must be zero or greater");
        }
    }
}
