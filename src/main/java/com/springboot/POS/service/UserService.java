package com.springboot.POS.service;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.User;

import java.util.List;

public interface UserService {
    User getUserFromJwtToken(String token) throws UserException;
    User getCurrentUser() throws UserException;
    User getUserByEmail(String email) throws UserException;
    User getUserById(Long id) throws UserException, Exception;
    List<User> getAllUser();
}
