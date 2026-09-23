package com.springboot.POS.service.impl;

import com.springboot.POS.configuration.JwtProvider;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.UserDTO;
import com.springboot.POS.modal.Store;
import com.springboot.POS.repository.BranchRepository;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreRepository storeRepository;

    @Override
    public User getUserFromJwtToken(String token) throws UserException {

        String email = jwtProvider.getEmailFromToken(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if(user == null){
            throw new UserException("Invalid Token ");
        }

        return user;
    }

    @Override
    public User updateOwnProfile(Long userId, UserDTO userDTO) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found"));

        if (userDTO.getFullName() != null && !userDTO.getFullName().isBlank()) {
            user.setFullName(userDTO.getFullName().trim());
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone().trim());
        }
        if (userDTO.getEmail() != null && !userDTO.getEmail().isBlank()
                && !userDTO.getEmail().equalsIgnoreCase(user.getEmail())) {
            Optional<User> existing = userRepository.findByEmail(userDTO.getEmail().trim());
            if (existing.isPresent()) {
                throw new UserException("Email is already in use");
            }
            user.setEmail(userDTO.getEmail().trim());
        }
        return userRepository.save(user);
    }

    @Override
    public User getCurrentUser() throws UserException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if ( user == null){
            throw new UserException("User not found");
        }
        return user;
    }

    @Override
    public User getUserByEmail(String email) throws UserException {
        User user = userRepository.findByEmail(email).orElse(null);
        if ( user == null){
            throw new UserException("User not found");
        }
        return user;
    }

    @Override
    public User getUserById(Long id) throws UserException, Exception {

        return userRepository.findById(id).orElseThrow(
                ()-> new Exception("User not found")
        );
    }
    @Override
    public User updateUser(Long id, UserDTO userDTO) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));

        if (userDTO.getFullName() != null) {
            user.setFullName(userDTO.getFullName());
        }
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone());
        }
        if (userDTO.getRole() != null) {
            user.setRole(userDTO.getRole());
        }
        if (userDTO.getStatus() != null) {
            user.setStatus(userDTO.getStatus());
        }
        if (userDTO.getPassword() != null && !userDTO.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        if (userDTO.getStoreId() != null) {
            Store store = storeRepository.findById(userDTO.getStoreId())
                    .orElseThrow(() -> new Exception("Store not found"));
            user.setStore(store);
        } else {
            user.setStore(null);
        }

        if (userDTO.getBranchId() != null) {
            Branch branch = branchRepository.findById(userDTO.getBranchId())
                    .orElseThrow(() -> new Exception("Branch not found"));
            user.setBranch(branch);
        } else {
            user.setBranch(null);
        }

        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUser() {
        return userRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .toList();
    }

    @Override
    public void updatePassword(String currentPassword, String newPassword) throws Exception {
        User user = getCurrentUser();
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new Exception("Current password is incorrect");
        }
        
        // Validate new password
        if (newPassword == null || newPassword.length() < 6) {
            throw new Exception("New password must be at least 6 characters long");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public User createStoreAdmin(String fullName, String email, String password, com.springboot.POS.modal.Store store) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(com.springboot.POS.domain.UserRole.ROLE_STORE_ADMIN);
        user.setStore(store);
        return userRepository.save(user);
    }

    @Override
    public User createStoreAdminWithEncodedPassword(String fullName, String email, String encodedPassword, com.springboot.POS.modal.Store store) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRole(com.springboot.POS.domain.UserRole.ROLE_STORE_ADMIN);
        user.setStore(store);
        return userRepository.save(user);
    }

    @Override
    public User createUser(UserDTO userDTO) throws Exception {
        User user = new User();
        user.setFullName(userDTO.getFullName());
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setRole(userDTO.getRole());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        
        if (userDTO.getStatus() != null) {
            user.setStatus(userDTO.getStatus());
        } else {
            user.setStatus("active");
        }

        if (userDTO.getStoreId() != null) {
            Store store = storeRepository.findById(userDTO.getStoreId())
                    .orElseThrow(() -> new Exception("Store not found"));
            user.setStore(store);
        }

        if (userDTO.getBranchId() != null) {
            Branch branch = branchRepository.findById(userDTO.getBranchId())
                    .orElseThrow(() -> new Exception("Branch not found"));
            user.setBranch(branch);
        }

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
        User current = getCurrentUser();
        if (current != null && current.getId().equals(user.getId())) {
            throw new UserException("You cannot delete your own account");
        }
        if (user.getRole() == com.springboot.POS.domain.UserRole.ROLE_ADMIN
                && !Boolean.TRUE.equals(user.getDeleted())) {
            long activeAdmins = userRepository.findByRole(com.springboot.POS.domain.UserRole.ROLE_ADMIN)
                    .stream().filter(u -> !Boolean.TRUE.equals(u.getDeleted())).count();
            if (activeAdmins <= 1) {
                throw new UserException("Cannot delete the last active administrator");
            }
        }
        user.setDeleted(true);
        userRepository.save(user);
    }

    @Override
    public User toggleUserStatus(Long id, String status) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
        user.setStatus(status);
        return userRepository.save(user);
    }
}
