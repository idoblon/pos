package com.springboot.POS.service;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.UserDTO;

import java.util.List;

public interface UserService {
    User getUserFromJwtToken(String token) throws UserException;
    User getCurrentUser() throws UserException;
    User getUserByEmail(String email) throws UserException;
    User getUserById(Long id) throws UserException, Exception;
    List<User> getAllUser();
    User updateUser(Long id, UserDTO userDTO) throws Exception;
}
