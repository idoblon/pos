package com.springboot.POS.payload.dto;

import com.springboot.POS.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {

    private Long id;

    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    private UserRole role;

    private String password;

    private Long branchId;

    private Long storeId;

    private String storeName;
    private String storeDescription;
    private String storeType;
    private String storeEmail;
    private String storePhone;
    private String storeAddress;
    private String storeOwnerName;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

}
