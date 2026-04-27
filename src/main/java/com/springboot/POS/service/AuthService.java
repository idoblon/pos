package com.springboot.POS.service;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.payload.dto.UserDTO;
import com.springboot.POS.payload.response.AuthResponse;

public interface AuthService {
    AuthResponse signup(UserDTO userDto) throws UserException;
    AuthResponse login(UserDTO userDto) throws UserException;

}
