package com.springboot.POS.service.impl;

import com.springboot.POS.domain.RestockStatus;
import com.springboot.POS.mapper.RestockRequestMapper;
import com.springboot.POS.modal.*;
import com.springboot.POS.payload.dto.RestockRequestDTO;
import com.springboot.POS.repository.*;
import com.springboot.POS.service.EmailService;
import com.springboot.POS.service.RestockRequestService;
import com.springboot.POS.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestockRequestServiceImpl implements RestockRequestService {

    private final RestockRequestRepository restockRequestRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final StockMovementService stockMovementService;

    @Override
    @Transactional
    public RestockRequestDTO createRequest(RestockRequestDTO requestDTO, User user) throws Exception {
        Branch branch = branchRepository.findById(requestDTO.getBranchId())
                .orElseThrow(() -> new Exception("Branch not found"));

        Product product = productRepository.findById(requestDTO.getProductId())
                .orElseThrow(() -> new Exception("Product not found"));

        // Get current stock from inventory
        Integer currentStock = inventoryRepository.findByProductIdAndBranchId(
                requestDTO.getProductId(), requestDTO.getBranchId()
        ).stream().findFirst().map(Inventory::getQuantity).orElse(0);

        RestockRequest request = RestockRequest.builder()
                .branch(branch)
                .product(product)
                .requestedBy(user)
                .requestedQuantity(requestDTO.getRequestedQuantity())
                .currentStock(currentStock)
                .notes(requestDTO.getNotes())
                .build();

        RestockRequest saved = restockRequestRepository.save(request);

        // Send email to store admin
        List<User> storeAdmins = userRepository.findByStore_IdAndDeletedFalse(branch.getStore().getId())
                .stream()
                .filter(u -> u.getRole().name().contains("STORE_ADMIN") || u.getRole().name().contains("STORE_MANAGER"))
                .toList();

        for (User admin : storeAdmins) {
            emailService.sendRestockRequestEmail(
                    admin.getEmail(),
                    admin.getFullName(),
                    branch.getName(),
                    product.getName(),
                    requestDTO.getRequestedQuantity(),
                    currentStock
            );
        }

        return RestockRequestMapper.toDTO(saved);
    }

    @Override
    public List<RestockRequestDTO> getRequestsByStore(Long storeId) {
        return restockRequestRepository.findByStoreId(storeId).stream()
                .map(RestockRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RestockRequestDTO> getRequestsByBranch(Long branchId) {
        return restockRequestRepository.findByBranchId(branchId).stream()
                .map(RestockRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RestockRequestDTO> getRequestsByStoreAndStatus(Long storeId, RestockStatus status) {
        return restockRequestRepository.findByStoreIdAndStatus(storeId, status).stream()
                .map(RestockRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RestockRequestDTO approveRequest(Long requestId, User user) throws Exception {
        RestockRequest request = restockRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Restock request not found"));

        if (request.getStatus() != RestockStatus.PENDING) {
            throw new Exception("Only pending requests can be approved");
        }

        request.setStatus(RestockStatus.APPROVED);
        request.setApprovedBy(user);

        RestockRequest saved = restockRequestRepository.save(request);

        // Send email to branch manager
        emailService.sendRestockApprovedEmail(
                request.getRequestedBy().getEmail(),
                request.getRequestedBy().getFullName(),
                request.getProduct().getName(),
                request.getRequestedQuantity()
        );

        return RestockRequestMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public RestockRequestDTO rejectRequest(Long requestId, String reason, User user) throws Exception {
        RestockRequest request = restockRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Restock request not found"));

        if (request.getStatus() != RestockStatus.PENDING) {
            throw new Exception("Only pending requests can be rejected");
        }

        request.setStatus(RestockStatus.REJECTED);
        request.setRejectionReason(reason);
        request.setApprovedBy(user);

        RestockRequest saved = restockRequestRepository.save(request);

        // Send email to branch manager
        emailService.sendRestockRejectedEmail(
                request.getRequestedBy().getEmail(),
                request.getRequestedBy().getFullName(),
                request.getProduct().getName(),
                reason
        );

        return RestockRequestMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public RestockRequestDTO fulfillRequest(Long requestId, Integer receivedQuantity, User user) throws Exception {
        System.out.println("🔍 FULFILL REQUEST DEBUG - Starting fulfillRequest for requestId: " + requestId);
        System.out.println("🔍 FULFILL REQUEST DEBUG - Received quantity: " + receivedQuantity);
        System.out.println("🔍 FULFILL REQUEST DEBUG - User: " + user.getFullName() + " (ID: " + user.getId() + ")");
        
        RestockRequest request = restockRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Restock request not found"));

        System.out.println("🔍 FULFILL REQUEST DEBUG - Found request: " + request.getId());
        System.out.println("🔍 FULFILL REQUEST DEBUG - Request status: " + request.getStatus());
        System.out.println("🔍 FULFILL REQUEST DEBUG - Product ID: " + request.getProduct().getId());
        System.out.println("🔍 FULFILL REQUEST DEBUG - Branch ID: " + request.getBranch().getId());
        System.out.println("🔍 FULFILL REQUEST DEBUG - Requested quantity: " + request.getRequestedQuantity());

        if (request.getStatus() != RestockStatus.APPROVED) {
            System.out.println("❌ FULFILL REQUEST DEBUG - Request status is not APPROVED: " + request.getStatus());
            throw new Exception("Only approved requests can be fulfilled");
        }

        // Use received quantity if provided, otherwise use requested quantity
        Integer quantityToAdd = (receivedQuantity != null && receivedQuantity > 0) 
            ? receivedQuantity 
            : request.getRequestedQuantity();
        
        System.out.println("🔍 FULFILL REQUEST DEBUG - Quantity to add: " + quantityToAdd);

        // Update inventory
        List<Inventory> inventoryList = inventoryRepository.findByProductIdAndBranchId(
                request.getProduct().getId(),
                request.getBranch().getId()
        );
        
        System.out.println("🔍 FULFILL REQUEST DEBUG - Found " + inventoryList.size() + " inventory records");
        
        if (inventoryList.isEmpty()) {
            System.out.println("❌ FULFILL REQUEST DEBUG - No inventory found for productId: " + request.getProduct().getId() + ", branchId: " + request.getBranch().getId());
            throw new Exception("Inventory not found for product " + request.getProduct().getName() + " in branch " + request.getBranch().getName());
        }
        
        Inventory inventory = inventoryList.get(0);
        System.out.println("🔍 FULFILL REQUEST DEBUG - Current inventory quantity: " + inventory.getQuantity());
        System.out.println("🔍 FULFILL REQUEST DEBUG - Inventory ID: " + inventory.getId());
        
        Integer oldQuantity = inventory.getQuantity();
        Integer newQuantity = oldQuantity + quantityToAdd;
        
        inventory.setQuantity(newQuantity);
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        System.out.println("🔍 FULFILL REQUEST DEBUG - Updated inventory from " + oldQuantity + " to " + newQuantity);
        System.out.println("🔍 FULFILL REQUEST DEBUG - Saved inventory quantity: " + savedInventory.getQuantity());

        // Record stock movement
        try {
            stockMovementService.recordMovement(
                    inventory.getId(),
                    com.springboot.POS.domain.StockMovementType.RESTOCK,
                    quantityToAdd,
                    receivedQuantity != null 
                        ? String.format("Restock fulfilled - Received %d of %d requested", receivedQuantity, request.getRequestedQuantity())
                        : "Restock request fulfilled",
                    "RestockRequest",
                    request.getId(),
                    user
            );
            System.out.println("✅ FULFILL REQUEST DEBUG - Stock movement recorded successfully");
        } catch (Exception e) {
            System.out.println("❌ FULFILL REQUEST DEBUG - Failed to record stock movement: " + e.getMessage());
            e.printStackTrace();
        }

        // Update request with received quantity
        request.setReceivedQuantity(quantityToAdd);
        request.setStatus(RestockStatus.FULFILLED);
        RestockRequest saved = restockRequestRepository.save(request);
        
        System.out.println("🔍 FULFILL REQUEST DEBUG - Updated request status to FULFILLED");
        System.out.println("🔍 FULFILL REQUEST DEBUG - Request received quantity set to: " + saved.getReceivedQuantity());

        // Send email to branch manager
        try {
            emailService.sendRestockFulfilledEmail(
                    request.getRequestedBy().getEmail(),
                    request.getRequestedBy().getFullName(),
                    request.getProduct().getName(),
                    quantityToAdd,
                    savedInventory.getQuantity()
            );
            System.out.println("✅ FULFILL REQUEST DEBUG - Email sent successfully");
        } catch (Exception e) {
            System.out.println("❌ FULFILL REQUEST DEBUG - Failed to send email: " + e.getMessage());
        }

        System.out.println("✅ FULFILL REQUEST DEBUG - fulfillRequest completed successfully");
        return RestockRequestMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public List<RestockRequestDTO> batchApprove(List<Long> requestIds, User user) throws Exception {
        return requestIds.stream()
                .map(id -> {
                    try {
                        return approveRequest(id, user);
                    } catch (Exception e) {
                        System.err.println("Failed to approve request " + id + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<RestockRequestDTO> batchReject(List<Long> requestIds, String reason, User user) throws Exception {
        return requestIds.stream()
                .map(id -> {
                    try {
                        return rejectRequest(id, reason, user);
                    } catch (Exception e) {
                        System.err.println("Failed to reject request " + id + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<RestockRequestDTO> batchFulfill(List<Long> requestIds, User user) throws Exception {
        return requestIds.stream()
                .map(id -> {
                    try {
                        return fulfillRequest(id, user);
                    } catch (Exception e) {
                        System.err.println("Failed to fulfill request " + id + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    // Debug method - remove in production
    @Override
    public InventoryRepository getInventoryRepository() {
        return inventoryRepository;
    }
}
