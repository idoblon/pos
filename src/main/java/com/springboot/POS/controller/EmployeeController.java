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
        ownershipGuard.requireBranchAccess(userService.getUserFromJwtToken(jwt), branchId);
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
        ownershipGuard.requireStoreAccess(currentUser, ownershipGuard.resolveStoreIdOf(target));
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
        ownershipGuard.requireStoreAccess(currentUser, ownershipGuard.resolveStoreIdOf(target));
        employeeService.deleteEmployee(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Employee deleted");
        return ResponseEntity.ok(apiResponse);
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
