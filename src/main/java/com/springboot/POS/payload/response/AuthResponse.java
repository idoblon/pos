package com.springboot.POS.payload.response;

import com.springboot.POS.payload.dto.UserDTO;
import lombok.Data;

@Data
public class AuthResponse {

    private String jwt;
    private String message;
    private UserDTO user;

}
