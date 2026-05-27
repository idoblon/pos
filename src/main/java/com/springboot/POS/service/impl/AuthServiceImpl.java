package com.springboot.POS.service.impl;

import com.springboot.POS.configuration.JwtProvider;
import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.UserMapper;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.UserDTO;
import com.springboot.POS.payload.response.AuthResponse;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository; // ✅ ADD THIS
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomerUserImplementation customeUserImplementation;

    @Override
    public AuthResponse signup(UserDTO userDto) throws UserException {
        if (userDto.getFullName() == null || userDto.getFullName().isBlank()) {
            throw new UserException("Full name is required");
        }
        if (userDto.getRole() == null) {
            throw new UserException("Role is required");
        }
        User user = userRepository.findByEmail(userDto.getEmail());
        if (user != null){
            throw new UserException("email id already registered ! ");
        }
        if (userDto.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new UserException("role admin is not allowed !");
        }

        // ✅ CREATE STORE FOR STORE_ADMIN FIRST
        Store store = null;
        if (userDto.getRole().equals(UserRole.ROLE_STORE_ADMIN) ||
                userDto.getRole().equals(UserRole.ROLE_STORE_MANAGER)) {

            if (userDto.getStoreName() == null || userDto.getStoreName().isBlank()) {
                throw new UserException("Store name is required for store admin");
            }

            // Create new store
            store = new Store();
            store.setBrand(userDto.getStoreName());
            store.setCreatedAt(LocalDateTime.now());
            store.setUpdatedAt(LocalDateTime.now());

            // Save store first (without storeAdmin reference yet)
            store = storeRepository.save(store);
        }

        // ✅ CREATE USER AND LINK TO STORE
        User newUser = new User();
        newUser.setEmail(userDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        newUser.setRole(userDto.getRole());
        newUser.setFullName(userDto.getFullName());
        newUser.setPhone(userDto.getPhone());
        newUser.setLastLogin(LocalDateTime.now());
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        // Link user to store
        if (store != null) {
            newUser.setStore(store);
        }

        User savedUser = userRepository.save(newUser);

        // ✅ UPDATE STORE WITH STORE_ADMIN REFERENCE
        if (store != null) {
            store.setStoreAdmin(savedUser);
            storeRepository.save(store);
        }

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDto.getEmail(),
                        userDto.getPassword());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateToken(authentication);

        // ✅ BUILD RESPONSE WITH STORE INFO
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Registered Successfully");
        authResponse.setUser(UserMapper.toDTO(savedUser));
        authResponse.setRole(savedUser.getRole());
        authResponse.setStoreId(savedUser.getStoreId()); // Uses the helper method
        authResponse.setBranchId(savedUser.getBranchId()); // Uses the helper method
        authResponse.setStoreName(store != null ? store.getBrand() : null);

        return authResponse;
    }

    @Override
    public AuthResponse login(UserDTO userDto) throws UserException {
        String email = userDto.getEmail();
        String password = userDto.getPassword();
        Authentication authentication = authenticate(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.iterator().next().getAuthority();
        String jwt = jwtProvider.generateToken(authentication);

        User user = userRepository.findByEmail(email);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // ✅ BUILD RESPONSE WITH STORE INFO
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Login Successfully");
        authResponse.setUser(UserMapper.toDTO(user));
        authResponse.setRole(user.getRole());
        authResponse.setStoreId(user.getStoreId()); // Uses the helper method
        authResponse.setBranchId(user.getBranchId()); // Uses the helper method
        authResponse.setStoreName(user.getStore() != null ? user.getStore().getBrand() : null);

        return authResponse;
    }

    @Override
    public AuthResponse refreshToken(String jwt) throws UserException {
        String email = jwtProvider.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("User not found");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, null, List.of(() -> user.getRole().name())
        );
        String newJwt = jwtProvider.generateToken(authentication);

        // ✅ BUILD RESPONSE WITH STORE INFO
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(newJwt);
        authResponse.setMessage("Token refreshed successfully");
        authResponse.setUser(UserMapper.toDTO(user));
        authResponse.setRole(user.getRole());
        authResponse.setStoreId(user.getStoreId()); // Uses the helper method
        authResponse.setBranchId(user.getBranchId()); // Uses the helper method
        authResponse.setStoreName(user.getStore() != null ? user.getStore().getBrand() : null);

        return authResponse;
    }

    private Authentication authenticate(String email, String password) throws UserException {
        UserDetails userDetails = customeUserImplementation.loadUserByUsername(email);

        if( userDetails == null ){
            throw  new UserException("email id doesn't exist" + email);
        }
        if(!passwordEncoder.matches(password, userDetails.getPassword())){
            throw new UserException("password doesn't match");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }


}
