package com.springboot.POS.controller;

import com.springboot.POS.modal.StorePaymentConfig;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StorePaymentConfigDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.repository.StorePaymentConfigRepository;
import com.springboot.POS.service.StorePaymentConfigService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment-config")
public class StorePaymentConfigController {

    private final StorePaymentConfigService paymentConfigService;
    private final UserService userService;
    private final StorePaymentConfigRepository paymentConfigRepository;
    private final OwnershipGuard ownershipGuard;

    private StorePaymentConfig requireConfigAccess(User user, Long id) throws Exception {
        StorePaymentConfig config = paymentConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment configuration not found"));
        if (config.getStore() == null || config.getStore().getId() == null) {
            throw new IllegalArgumentException("Payment configuration has no store scope");
        }
        ownershipGuard.requireStoreAccess(user, config.getStore().getId());
        return config;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
    public ResponseEntity<StorePaymentConfig> configurePaymentMethod(
            @RequestBody StorePaymentConfigDTO configDTO,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        // Ensure user has permission (store admin)
        
        StorePaymentConfig config = paymentConfigService.configurePaymentMethod(
            user.getStoreId(), 
            configDTO
        );
        
        return ResponseEntity.ok(config);
    }

    @GetMapping("/store")
    public ResponseEntity<List<StorePaymentConfig>> getStorePaymentConfigs(
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.getUserFromJwtToken(jwt);
        List<StorePaymentConfig> configs = paymentConfigService.getStorePaymentConfigs(resolveStoreId(user));

        return ResponseEntity.ok(configs);
    }

    @GetMapping("/store/enabled")
    public ResponseEntity<List<StorePaymentConfig>> getEnabledPaymentMethods(
            @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.getUserFromJwtToken(jwt);
        List<StorePaymentConfig> configs = paymentConfigService.getEnabledPaymentMethods(resolveStoreId(user));

        return ResponseEntity.ok(configs);
    }

    /**
     * Branch-only users (cashiers, branch managers) carry their store on the
     * branch, not on the user row — resolve through it so enabled-methods
     * lookups work for the POS terminal.
     */
    private Long resolveStoreId(User user) throws Exception {
        Long storeId = user.getStoreId();
        if (storeId == null && user.getBranch() != null && user.getBranch().getStore() != null) {
            storeId = user.getBranch().getStore().getId();
        }
        if (storeId == null) throw new IllegalArgumentException("Unable to resolve store from token");
        return storeId;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
    public ResponseEntity<StorePaymentConfig> updatePaymentConfig(
            @PathVariable Long id,
            @RequestBody StorePaymentConfigDTO configDTO,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        requireConfigAccess(user, id);
        StorePaymentConfig config = paymentConfigService.updatePaymentConfig(id, configDTO);
        
        return ResponseEntity.ok(config);
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse> togglePaymentMethod(
            @PathVariable Long id,
            @RequestParam Boolean isEnabled,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        requireConfigAccess(user, id);
        paymentConfigService.togglePaymentMethod(id, isEnabled);
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Payment method " + (isEnabled ? "enabled" : "disabled") + " successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse> deletePaymentConfig(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        requireConfigAccess(user, id);
        paymentConfigService.deletePaymentConfig(id);
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Payment configuration deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse> initializeDefaultPaymentMethods(
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        paymentConfigService.initializeDefaultPaymentMethods(user.getStoreId());
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Default payment methods initialized successfully");
        return ResponseEntity.ok(response);
    }
}