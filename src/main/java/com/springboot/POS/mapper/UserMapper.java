package com.springboot.POS.mapper;

import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.UserDTO;

public class UserMapper {
    public static UserDTO toDTO(User savedUser) {
        UserDTO userDto = new UserDTO();
        userDto.setId(savedUser.getId());
        userDto.setFullName(savedUser.getFullName());
        userDto.setEmail(savedUser.getEmail());
        userDto.setRole(savedUser.getRole());
        userDto.setCreatedAt(savedUser.getCreatedAt());
        userDto.setUpdatedAt(savedUser.getUpdatedAt());
        userDto.setLastLogin(savedUser.getLastLogin());
        userDto.setPhone(savedUser.getPhone());
        userDto.setStoreId(savedUser.getStore()!=null? savedUser.getStore().getId():null);
        userDto.setBranchId(savedUser.getBranch()!=null? savedUser.getBranch().getId():null);

        return userDto;
    }

    public static User toEntity(UserDTO userDTO) {
        User createdUser = new User();
        createdUser.setEmail(userDTO.getEmail());
        createdUser.setFullName(userDTO.getFullName());
        createdUser.setRole(userDTO.getRole());
        createdUser.setCreatedAt(userDTO.getCreatedAt());
        createdUser.setUpdatedAt(userDTO.getUpdatedAt());
        createdUser.setLastLogin(userDTO.getLastLogin());
        createdUser.setPhone(userDTO.getPhone());
        createdUser.setPassword(userDTO.getPassword());

        return createdUser;
    }
}
