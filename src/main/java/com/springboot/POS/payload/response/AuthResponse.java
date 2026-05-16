package com.springboot.POS.payload.response;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.payload.dto.UserDTO;
import lombok.Data;

@Data
public class AuthResponse {

    private String jwt;
    private String message;
    private UserDTO user;
    private UserRole role;
    private Long storeId;
    private Long branchId;
    private String storeName;

}
