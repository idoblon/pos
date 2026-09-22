package com.springboot.POS.controller;

import com.springboot.POS.modal.Order;
import com.springboot.POS.modal.ShiftReport;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.RefundDTO;
import com.springboot.POS.repository.OrderRepository;
import com.springboot.POS.repository.ShiftReportRepository;
import com.springboot.POS.service.RefundService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import com.springboot.POS.util.QueryLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;
    private final OrderRepository orderRepository;
    private final ShiftReportRepository shiftReportRepository;

    @PostMapping
    public ResponseEntity<RefundDTO> createRefund(
            @RequestBody RefundDTO refundDTO,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        // A refund must be scoped to an order of the requester's own branch.
        Order order = orderRepository.findById(refundDTO.getOrderId())
                .orElseThrow(() -> new Exception("Order not found"));
        if (order.getBranch() != null) {
            ownershipGuard.requireBranchAccess(user, order.getBranch().getId());
        }
        RefundDTO refund = refundService.createRefund(refundDTO);
        return ResponseEntity.ok(refund);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RefundDTO>> getAllRefund(
            @RequestParam(required = false, defaultValue = "1000") int limit) throws Exception {
        List<RefundDTO> refund = QueryLimits.mostRecent(
                refundService.getAllRefunds(), RefundDTO::getCreatedAt, limit);
        return ResponseEntity.ok(refund);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundDTO> getRefundById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        RefundDTO refund = refundService.getRefundById(id);
        if (refund.getBranchId() != null) {
            ownershipGuard.requireBranchAccess(userService.getUserFromJwtToken(jwt), refund.getBranchId());
        } else if (refund.getCashier() != null && refund.getCashier().getId() != null) {
            ownershipGuard.requireUserAccess(userService.getUserFromJwtToken(jwt), refund.getCashier().getId());
        }
        return ResponseEntity.ok(refund);
    }

    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<List<RefundDTO>> getRefundByCashier(
            @PathVariable Long cashierId,
            @RequestHeader("Authorization") String jwt
    ) throws Exception {
        ownershipGuard.requireUserAccess(userService.getUserFromJwtToken(jwt), cashierId);
        List<RefundDTO> refund = QueryLimits.mostRecent(
                refundService.getRefundByCashier(cashierId), RefundDTO::getCreatedAt, 1000);
        return ResponseEntity.ok(refund);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<RefundDTO>> getRefundByBranch(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt
    ) throws Exception {
        ownershipGuard.requireBranchAccess(userService.getUserFromJwtToken(jwt), branchId);
        List<RefundDTO> refund = QueryLimits.mostRecent(
                refundService.getRefundByBranch(branchId), RefundDTO::getCreatedAt, 1000);
        return ResponseEntity.ok(refund);
    }

    @GetMapping("/monthly/branch/{branchId}")
    public ResponseEntity<List<RefundDTO>> getMonthlyRefundsByBranch(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt
    ) throws Exception {
        ownershipGuard.requireBranchAccess(userService.getUserFromJwtToken(jwt), branchId);
        return ResponseEntity.ok(refundService.getMonthlyRefundsByBranch(branchId));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<RefundDTO>> getRefundsByStore(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt
    ) throws Exception {
        ownershipGuard.requireStoreAccess(userService.getUserFromJwtToken(jwt), storeId);
        return ResponseEntity.ok(QueryLimits.mostRecent(
                refundService.getRefundsByStore(storeId), RefundDTO::getCreatedAt, 1000));
    }

    @GetMapping("/shift/{shiftId}")
    public ResponseEntity<List<RefundDTO>> getRefundByShift(
            @PathVariable Long shiftId,
            @RequestHeader("Authorization") String jwt
    ) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ShiftReport report = shiftReportRepository.findById(shiftId)
                .orElseThrow(() -> new Exception("Shift report not found"));
        if (report.getCashier() != null) {
            ownershipGuard.requireUserAccess(user, report.getCashier().getId());
        }
        if (report.getBranch() != null) {
            ownershipGuard.requireBranchAccess(user, report.getBranch().getId());
        }
        return ResponseEntity.ok(refundService.getRefundByShiftReport(shiftId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRefund(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        RefundDTO refund = refundService.getRefundById(id);
        if (refund.getBranchId() != null) {
            ownershipGuard.requireBranchAccess(userService.getUserFromJwtToken(jwt), refund.getBranchId());
        } else {
            ownershipGuard.requireUserAccess(userService.getUserFromJwtToken(jwt),
                    refund.getCashier() != null ? refund.getCashier().getId() : null);
        }
        refundService.deleteRefund(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cashier/{cashierId}/range")
    public ResponseEntity<List<RefundDTO>> getRefundByCashierAndDateRange(
            @PathVariable Long cashierId,
            @RequestHeader("Authorization") String jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) throws Exception {
        ownershipGuard.requireUserAccess(userService.getUserFromJwtToken(jwt), cashierId);
        List<RefundDTO> refund = QueryLimits.mostRecent(
                refundService.getRefundByCashierIdAndDateRange(cashierId, startDate, endDate),
                RefundDTO::getCreatedAt, 1000);
        return ResponseEntity.ok(refund);
    }
}
