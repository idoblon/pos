package com.springboot.POS.service;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.payload.dto.UserDto;
import com.springboot.POS.payload.response.AuthResponse;

public interface AuthService {
    AuthResponse signup(UserDto userDto) throws UserException;
    AuthResponse login(UserDto userDto)

}
