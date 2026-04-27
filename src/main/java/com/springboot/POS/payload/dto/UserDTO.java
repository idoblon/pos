package com.springboot.POS.payload.dto;

import com.springboot.POS.domain.UserRole;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class UserDTO {

    private Long id;

    private  String fullName;

    private String email;

    private String phone;

    private UserRole role;

    private String password;

    private Long branchId;

    private Long StoreId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

}
