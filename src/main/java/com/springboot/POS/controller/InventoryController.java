package com.springboot.POS.controller;

import com.springboot.POS.payload.dto.InventoryDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping()
    public ResponseEntity<InventoryDTO> create(
            @RequestBody InventoryDTO inventoryDTO
    ) throws Exception{
        return ResponseEntity.ok(inventoryService.createInventory(inventoryDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryDTO> update(
            @RequestBody InventoryDTO inventoryDTO,
            @PathVariable Long id
    ) throws Exception{
        return ResponseEntity.ok(inventoryService.updateInventory(id, inventoryDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(
            @PathVariable Long id
    ) throws Exception{
        inventoryService.deleteInventory(id);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Inventory deleted");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<InventoryDTO> getInventoryByProductAndBranchId(
            @PathVariable Long branchId,
            @PathVariable Long productId
    ) throws Exception{
        return ResponseEntity.ok(inventoryService
                .getInventoryByProductAndBranchId(productId, branchId));
    }


    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<InventoryDTO>> getInventoryByBranch(

            @PathVariable Long branchId
    ) throws Exception{
        return ResponseEntity.ok(inventoryService.getAllInventoryByBranchId(branchId));
    }

}
