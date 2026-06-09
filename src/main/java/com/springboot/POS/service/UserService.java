package com.springboot.POS.service;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Store;
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
    void updatePassword(String currentPassword, String newPassword) throws Exception;
    User createStoreAdmin(String fullName, String email, String password, Store store);
    User createStoreAdminWithEncodedPassword(String fullName, String email, String encodedPassword, Store store);
}
