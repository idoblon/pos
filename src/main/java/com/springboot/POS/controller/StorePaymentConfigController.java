package com.springboot.POS.controller;

import com.springboot.POS.modal.StorePaymentConfig;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StorePaymentConfigDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.StorePaymentConfigService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment-config")
public class StorePaymentConfigController {

    private final StorePaymentConfigService paymentConfigService;
    private final UserService userService;

    @PostMapping
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
        List<StorePaymentConfig> configs = paymentConfigService.getStorePaymentConfigs(user.getStoreId());
        
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/store/enabled")
    public ResponseEntity<List<StorePaymentConfig>> getEnabledPaymentMethods(
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        List<StorePaymentConfig> configs = paymentConfigService.getEnabledPaymentMethods(user.getStoreId());
        
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StorePaymentConfig> updatePaymentConfig(
            @PathVariable Long id,
            @RequestBody StorePaymentConfigDTO configDTO,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        StorePaymentConfig config = paymentConfigService.updatePaymentConfig(id, configDTO);
        
        return ResponseEntity.ok(config);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse> togglePaymentMethod(
            @PathVariable Long id,
            @RequestParam Boolean isEnabled,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        paymentConfigService.togglePaymentMethod(id, isEnabled);
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Payment method " + (isEnabled ? "enabled" : "disabled") + " successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletePaymentConfig(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        paymentConfigService.deletePaymentConfig(id);
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Payment configuration deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse> initializeDefaultPaymentMethods(
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        paymentConfigService.initializeDefaultPaymentMethods(user.getStoreId());
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Default payment methods initialized successfully");
        return ResponseEntity.ok(response);
    }
}