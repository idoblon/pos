package com.springboot.POS.controller;

import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.StoreMapper;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StoreDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.StoreService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;

    @PostMapping
    public ResponseEntity<StoreDTO> createStore(@RequestBody StoreDTO storeDTO,
                                                @RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(storeService.createStore(storeDTO, user));
    }

    @GetMapping
    public ResponseEntity<List<StoreDTO>> getAllStores(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);

        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return ResponseEntity.ok(storeService.getAllStoreDTOs());
        }
        // Non-admins only ever see their own store — never the full tenant list.
        Store own = user.getStore() != null ? user.getStore()
                : (user.getBranch() != null ? user.getBranch().getStore() : null);
        if (own == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(List.of(StoreMapper.toDTO(own)));
    }

    @GetMapping("/admin")
    public ResponseEntity<StoreDTO> getStoreByAdmin(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        Store store = storeService.getStoreByAdmin();
        return ResponseEntity.ok(StoreMapper.toDTO(store));
    }

    @GetMapping("/employee")
    public ResponseEntity<StoreDTO> getStoreByEmployee(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(storeService.getStoreByEmployee());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreDTO> updateStore(@PathVariable Long id,
                                                @RequestBody StoreDTO storeDTO,
                                                @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, id);
        return ResponseEntity.ok(storeService.updateStore(id, storeDTO));
    }

    @PutMapping("/{id}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreDTO> moderateStore(@PathVariable Long id,
                                                  @RequestParam StoreStatus status,
                                                  @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(storeService.moderateStore(id, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDTO> getStoreById(@PathVariable Long id,
                                                 @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, id);
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteStore(@PathVariable Long id,
                                                   @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        storeService.deleteStore(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Store deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/admin/backfill-subscription-dates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> backfillSubscriptionDates(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        int count = storeService.backfillSubscriptionDates();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Backfilled subscription dates for " + count + " stores.");
        apiResponse.setSuccess(true);
        return ResponseEntity.ok(apiResponse);
    }
}
