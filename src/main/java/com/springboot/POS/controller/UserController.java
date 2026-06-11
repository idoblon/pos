package com.springboot.POS.controller;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.UserMapper;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.UserDTO;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.payload.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/api/users/profile")
    public ResponseEntity<UserDTO> getUserProfile(
            @RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @GetMapping("/api/users/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long id) throws UserException, Exception {
        User currentUser = userService.getUserFromJwtToken(jwt);
        User user = userService.getUserById(id);
        if (user == null) {
            throw new UserException("User not found");
        }
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @PutMapping("/api/users/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long id,
            @RequestBody UserDTO userDTO) throws UserException, Exception {
        User updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(UserMapper.toDTO(updatedUser));
    }

    @GetMapping("/users/list")
    public ResponseEntity<List<User>> getUserList(
           ) throws UserException, Exception {
        List<User> users = userService.getAllUser();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(required = false) UserRole role) {
        List<User> users = userService.getAllUser();
        if (role != null) {
            users = users.stream()
                    .filter(u -> u.getRole() == role)
                    .toList();
        }
        return ResponseEntity.ok(users.stream()
                .map(UserMapper::toDTO)
                .toList());
    }

    @GetMapping("/api/users/store-admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getStoreAdmins() {
        List<User> users = userService.getAllUser().stream()
                .filter(u -> u.getRole() == UserRole.ROLE_STORE_ADMIN)
                .toList();
        return ResponseEntity.ok(users.stream()
                .map(UserMapper::toDTO)
                .toList());
    }

    @PostMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUser(
            @RequestBody UserDTO userDTO) throws Exception {
        User user = userService.createUser(userDTO);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @DeleteMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteUser(
            @PathVariable Long id) throws Exception {
        userService.deleteUser(id);
        ApiResponse response = new ApiResponse();
        response.setMessage("User deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) throws Exception {
        String status = body.get("status");
        User user = userService.toggleUserStatus(id, status);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }
    
    @PutMapping("/api/users/update-password")
    public ResponseEntity<String> updatePassword(
            @RequestHeader("Authorization") String jwt,
            @RequestBody UpdatePasswordRequest request) throws Exception {
        userService.updatePassword(request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok("Password updated successfully");
    }

    @PutMapping("/api/users/change-password")
    public ResponseEntity<String> changePassword(
            @RequestHeader("Authorization") String jwt,
            @RequestBody UpdatePasswordRequest request) throws Exception {
        return updatePassword(jwt, request);
    }
    
    // Inner class for password update request
    public static class UpdatePasswordRequest {
        private String currentPassword;
        private String newPassword;
        
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}