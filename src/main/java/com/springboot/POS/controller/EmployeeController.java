package com.springboot.POS.controller;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.mapper.UserMapper;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.UserDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.EmployeeService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;

    @PostMapping("/store/{storeId}")
    public ResponseEntity<UserDTO> createStoreEmployee(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt,
            @Valid @RequestBody UserDTO userDTO) throws Exception {
        ownershipGuard.requireStoreAccess(userService.getUserFromJwtToken(jwt), storeId);
        UserDTO employee = employeeService.createStoreEmployee(userDTO, storeId);
        return ResponseEntity.ok(employee);
    }

    @PostMapping("/branch/{branchId}")
    public ResponseEntity<UserDTO> createBranchEmployee(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt,
            @Valid @RequestBody UserDTO userDTO) throws Exception {
        User requester = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(requester, branchId);
        if (requester.getRole() == com.springboot.POS.domain.UserRole.ROLE_BRANCH_MANAGER
                && userDTO.getRole() != com.springboot.POS.domain.UserRole.ROLE_BRANCH_CASHIER) {
            throw new com.springboot.POS.exceptions.ResourceAccessDeniedException(
                    "Branch managers can only create cashier accounts; managers are created by store admin");
        }
        UserDTO employee = employeeService.createBranchEmployee(userDTO, branchId);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getEmployee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User currentUser = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireUserAccess(currentUser, id);
        User employee = userService.getUserById(id);
        if (employee == null) {
            throw new com.springboot.POS.exceptions.UserException("Employee not found");
        }
        return ResponseEntity.ok(UserMapper.toDTO(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateEmployee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt,
            @RequestBody UserDTO userDTO) throws Exception {
        User currentUser = userService.getUserFromJwtToken(jwt);
        User target = userService.getUserById(id);
        if (target == null) {
            throw new com.springboot.POS.exceptions.UserException("Employee not found");
        }
        requireEmployeeMutationAccess(currentUser, target, userDTO);
        User employee = employeeService.updateEmployee(id, userDTO);
        return ResponseEntity.ok(UserMapper.toDTO(employee));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteEmployee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User currentUser = userService.getUserFromJwtToken(jwt);
        User target = userService.getUserById(id);
        if (target == null) {
            throw new com.springboot.POS.exceptions.UserException("Employee not found");
        }
        requireEmployeeMutationAccess(currentUser, target, null);
        employeeService.deleteEmployee(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Employee deleted");
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Store roles keep the previous store-wide check. Branch managers are
     * scoped to their own branch: they may only mutate branch cashiers (or
     * managers of their own branch), may never touch store/admin roles or
     * other branches, and may never change roles or move branches.
     */
    private void requireEmployeeMutationAccess(User requester, User target,
                                               UserDTO userDTO) throws Exception {
        if (requester.getRole() != com.springboot.POS.domain.UserRole.ROLE_BRANCH_MANAGER) {
            ownershipGuard.requireStoreAccess(requester, ownershipGuard.resolveStoreIdOf(target));
            return;
        }
        Long requesterBranch = requester.getBranch() != null ? requester.getBranch().getId() : null;
        Long targetBranch = target.getBranch() != null ? target.getBranch().getId() : null;
        if (requesterBranch == null || targetBranch == null || !requesterBranch.equals(targetBranch)) {
            throw new com.springboot.POS.exceptions.ResourceAccessDeniedException(
                    "Branch managers can only manage employees of their own branch");
        }
        com.springboot.POS.domain.UserRole targetRole = target.getRole();
        if (targetRole == com.springboot.POS.domain.UserRole.ROLE_ADMIN
                || targetRole == com.springboot.POS.domain.UserRole.ROLE_STORE_ADMIN
                || targetRole == com.springboot.POS.domain.UserRole.ROLE_STORE_MANAGER) {
            throw new com.springboot.POS.exceptions.ResourceAccessDeniedException(
                    "Branch managers cannot manage store-level accounts");
        }
        if (userDTO != null && userDTO.getRole() != null && userDTO.getRole() != targetRole) {
            throw new com.springboot.POS.exceptions.ResourceAccessDeniedException(
                    "Branch managers cannot change employee roles");
        }
        if (userDTO != null && userDTO.getBranchId() != null
                && !userDTO.getBranchId().equals(targetBranch)) {
            throw new com.springboot.POS.exceptions.ResourceAccessDeniedException(
                    "Branch managers cannot move employees between branches");
        }
    }

    @GetMapping("/store/{id}")
    public ResponseEntity<List<UserDTO>> storeEmployee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt,
            @RequestParam(required = false) UserRole userRole) throws Exception {
        ownershipGuard.requireStoreAccess(userService.getUserFromJwtToken(jwt), id);
        List<UserDTO> employee = employeeService.findStoreEmployees(id, userRole);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/branch/{id}")
    public ResponseEntity<List<UserDTO>> branchEmployee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt,
            @RequestParam(required = false) UserRole userRole) throws Exception {
        ownershipGuard.requireBranchAccess(userService.getUserFromJwtToken(jwt), id);
        List<UserDTO> employee = employeeService.findBranchEmployees(id, userRole);
        return ResponseEntity.ok(employee);
    }
}
