package com.springboot.POS.controller;

import com.springboot.POS.domain.OrderStatus;
import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.ResourceAccessDeniedException;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.OrderDTO;
import com.springboot.POS.repository.CustomerRepository;
import com.springboot.POS.service.OrderService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import com.springboot.POS.util.QueryLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;
    private final CustomerRepository customerRepository;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @RequestBody OrderDTO order,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User caller = userService.getUserFromJwtToken(jwt);
        if (order.getBranchId() != null) {
            ownershipGuard.requireBranchAccess(caller, order.getBranchId());
        }
        try {
            return ResponseEntity.ok(orderService.createOrder(order, idempotencyKey));
        } catch (DataIntegrityViolationException ex) {
            // Unique idempotency_key constraint: a concurrent identical request won the race.
            OrderDTO winner = orderService.getOrderByIdempotencyKey(idempotencyKey);
            if (winner != null) return ResponseEntity.ok(winner);
            throw ex;
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders(
            @RequestHeader("Authorization") String jwt,
            @RequestParam(required = false, defaultValue = "1000") int limit) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        List<OrderDTO> orders;
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            orders = orderService.getAllOrders(limit);
        } else if (user.getRole() == UserRole.ROLE_BRANCH_CASHIER) {
            // Cashiers see only their own orders here; branch/store views
            // have dedicated guarded endpoints.
            orders = orderService.getOrderByCashier(user.getId());
        } else {
            Long storeId = ownershipGuard.resolveStoreIdOf(user);
            if (storeId != null) {
                orders = orderService.getOrdersByStore(storeId);
            } else if (user.getBranch() != null) {
                orders = orderService.getOrdersByBranch(user.getBranch().getId(), null, null, null, null);
            } else {
                orders = orderService.getOrderByCashier(user.getId());
            }
            orders = QueryLimits.mostRecent(orders, OrderDTO::getCreatedAt, limit);
        }
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/held")
    public ResponseEntity<OrderDTO> holdOrder(
            @RequestBody OrderDTO order,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User caller = userService.getUserFromJwtToken(jwt);
        if (order.getBranchId() != null) {
            ownershipGuard.requireBranchAccess(caller, order.getBranchId());
        }
        return ResponseEntity.ok(orderService.holdOrder(order));
    }

    @GetMapping("/held")
    public ResponseEntity<List<OrderDTO>> getHeldOrders(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User caller = userService.getUserFromJwtToken(jwt);
        List<OrderDTO> held = orderService.getHeldOrders();
        // Branch-scoped roles only see their own branch's held orders.
        Long callerBranch = caller.getBranch() != null ? caller.getBranch().getId() : null;
        if (callerBranch != null && caller.getRole() != UserRole.ROLE_ADMIN) {
            Long pinned = callerBranch;
            held = held.stream()
                    .filter(o -> o.getBranchId() == null || pinned.equals(o.getBranchId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(held);
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<OrderDTO> resumeHeldOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User caller = userService.getUserFromJwtToken(jwt);
        OrderDTO held = orderService.getOrderById(id);
        if (held.getBranchId() != null) {
            ownershipGuard.requireBranchAccess(caller, held.getBranchId());
        }
        return ResponseEntity.ok(orderService.resumeHeldOrder(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> discardHeldOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User caller = userService.getUserFromJwtToken(jwt);
        OrderDTO held = orderService.getOrderById(id);
        if (held.getBranchId() != null) {
            ownershipGuard.requireBranchAccess(caller, held.getBranchId());
        }
        orderService.discardHeldOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        OrderDTO order = orderService.getOrderById(id);
        if (order.getBranchId() == null) {
            if (user.getRole() != UserRole.ROLE_ADMIN) {
                throw new ResourceAccessDeniedException("Access denied: order has no branch scope");
            }
        } else {
            ownershipGuard.requireBranchAccess(user, order.getBranchId());
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByBranch(
            @PathVariable Long branchId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) PaymentType paymentType,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestHeader("Authorization") String jwt
    ) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(QueryLimits.mostRecent(
                orderService.getOrdersByBranch(branchId, customerId, cashierId, paymentType, orderStatus),
                OrderDTO::getCreatedAt, 1000));
    }

    @GetMapping("/cashier/{id}")
    public ResponseEntity<List<OrderDTO>> getOrderByCashier(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        ownershipGuard.requireUserAccess(userService.getUserFromJwtToken(jwt), id);
        return ResponseEntity.ok(QueryLimits.mostRecent(
                orderService.getOrderByCashier(id), OrderDTO::getCreatedAt, 1000));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<List<OrderDTO>> getOrdersByStore(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        ownershipGuard.requireStoreAccess(userService.getUserFromJwtToken(jwt), storeId);
        return ResponseEntity.ok(QueryLimits.mostRecent(
                orderService.getOrdersByStore(storeId), OrderDTO::getCreatedAt, 1000));
    }

    @GetMapping("/monthly/branch/{branchId}")
    public ResponseEntity<List<OrderDTO>> getMonthlyOrdersByBranch(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(orderService.getMonthlyOrdersByBranch(branchId));
    }

    @GetMapping("/monthly/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<List<OrderDTO>> getMonthlyOrdersByStore(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        ownershipGuard.requireStoreAccess(userService.getUserFromJwtToken(jwt), storeId);
        return ResponseEntity.ok(orderService.getMonthlyOrdersByStore(storeId));
    }

    @GetMapping("/today/branch/{id}")
    public ResponseEntity<List<OrderDTO>> getTodayOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, id);
        return ResponseEntity.ok(orderService.getTodayOrdersByBranch(id));
    }

    @GetMapping("/customer/{id}")
    public ResponseEntity<List<OrderDTO>> getCustomersOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            com.springboot.POS.modal.Customer customer = customerRepository.findById(id)
                    .orElseThrow(() -> new ResourceAccessDeniedException("Customer not found"));
            if (customer.getStoreId() == null) {
                throw new ResourceAccessDeniedException("Access denied: customer has no store scope");
            }
            ownershipGuard.requireStoreAccess(user, customer.getStoreId());
        }
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(id));
    }

    @GetMapping("/recent/{branchId}")
    public ResponseEntity<List<OrderDTO>> getRecentOrder(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(orderService.getTop5RecentOrdersByBranchId(branchId));
    }
}
