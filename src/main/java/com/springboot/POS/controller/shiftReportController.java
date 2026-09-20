package com.springboot.POS.controller;

import com.springboot.POS.payload.dto.ShiftReportDTO;
import com.springboot.POS.repository.ShiftReportRepository;
import com.springboot.POS.service.ShiftReportService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shift-reports")
public class shiftReportController {

    private final ShiftReportService shiftReportService;
    private final ShiftReportRepository shiftReportRepository;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;

    @PostMapping("/start")
    public ResponseEntity<ShiftReportDTO> startShift(
            @RequestParam(required = false, defaultValue = "0") Double openingFloat
    ) throws Exception {
        return ResponseEntity.ok(shiftReportService.startShift(openingFloat));
    }

    @PatchMapping("/end")
    public ResponseEntity<ShiftReportDTO> endShift(
            @RequestParam(required = false, defaultValue = "0") Double declaredCash
    ) throws Exception {
        return ResponseEntity.ok(
                shiftReportService.endShift(null, LocalDateTime.now(), declaredCash)
        );
    }
    @GetMapping("/current")
    public ResponseEntity<ShiftReportDTO> getCurrentShiftProgress() throws Exception {
        ShiftReportDTO dto = shiftReportService.getCurrentShiftReportProgress();
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/handover")
    public ResponseEntity<ShiftReportDTO> saveHandover(
            @PathVariable Long id,
            @RequestBody ShiftHandoverRequest request
    ) throws Exception {
        return ResponseEntity.ok(shiftReportService.saveHandover(
                id, request.cashCounted(), request.notes(), request.nextTasks()));
    }

    public record ShiftHandoverRequest(Double cashCounted, String notes, String nextTasks) {}

    @GetMapping("/latest-handover")
    public ResponseEntity<ShiftReportDTO> getLatestHandover() throws Exception {
        ShiftReportDTO handover = shiftReportService.getLatestHandoverForCurrentBranch();
        return handover == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(handover);
    }
    @GetMapping("/cashier/{cashierId}/by-date")
    public ResponseEntity<ShiftReportDTO> getShiftReportByDate(
            @PathVariable Long cashierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime date
            ) throws Exception {
        return ResponseEntity.ok(
                shiftReportService.getShiftByCashierAndDate(cashierId, date)
        );
    }
    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<List<ShiftReportDTO>> getShiftReportByCashier(
            @PathVariable Long cashierId
    ) throws Exception {
        return ResponseEntity.ok(
                shiftReportService.getShiftReportByCashierId(cashierId)
        );
    }
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ShiftReportDTO>> getShiftReportByBranch(
            @PathVariable Long branchId
    ) throws Exception {
        return ResponseEntity.ok(
                shiftReportService.getShiftReportByBranchId(branchId)
        );
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<ShiftReportDTO>> getShiftReportByStore(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt
    ) throws Exception {
        ownershipGuard.requireStoreAccess(userService.getUserFromJwtToken(jwt), storeId);
        return ResponseEntity.ok(
                shiftReportRepository.findByStoreId(storeId).stream()
                        .map(com.springboot.POS.mapper.ShiftReportMapper::toDTO)
                        .collect(java.util.stream.Collectors.toList())
        );
    }



    @GetMapping("/{id}")
    public ResponseEntity<ShiftReportDTO> getShiftReportById(
            @PathVariable Long id
    ) throws Exception {
        return ResponseEntity.ok(
                shiftReportService.getShiftReportById(id)
        );
    }
}
