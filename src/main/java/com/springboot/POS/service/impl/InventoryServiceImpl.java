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
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final WebInvocationPrivilegeEvaluator webInvocationPrivilegeEvaluator;

    @Override
    public InventoryDTO createInventory(InventoryDTO inventoryDTO) throws Exception {

        Branch branch = branchRepository.findById(inventoryDTO.getBranchId()).orElseThrow(
                ()-> new Exception("branch does not exist....")
        );
        Product product = productRepository.findById(inventoryDTO.getProductId()).orElseThrow(
                () -> new Exception("product doesn't exist....")
        );

        Inventory inventory = InventoryMapper.toEntity(inventoryDTO,branch,product);
        Inventory savedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toDTO(savedInventory);
    }

    @Override
    public InventoryDTO updateInventory(Long id, InventoryDTO inventoryDTO) throws Exception {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(
                () -> new Exception("inventory not found....")
        );
        inventory.setQuantity(inventoryDTO.getQuantity());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toDTO(updatedInventory);
    }

    @Override
    public void deleteInventory(Long id) throws Exception {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(
                () -> new Exception("inventory not found....")
        );
        inventoryRepository.delete(inventory);
    }

    @Override
    public InventoryDTO getInventoryById(Long id) throws Exception {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(
                () -> new Exception("inventory not found....")
        );
        return null;
    }

    @Override
    public InventoryDTO getInventoryByProductAndBranchId(Long productId, Long branchId) {
        Inventory inventory = inventoryRepository.findByProductIdAndBranchId(productId,branchId);
        return InventoryMapper.toDTO(inventory);
    }

    @Override
    public List<InventoryDTO> getAllInventoryByBranchId(Long branchId) {
        List<Inventory> inventories = inventoryRepository.findByBranchId(branchId);
        return inventories.stream().map(
                InventoryMapper::toDTO
        ).collect(Collectors.toList());
    }
}
