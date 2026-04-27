package com.springboot.POS.service.impl;

import com.springboot.POS.configuration.JwtProvider;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.User;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public User getUserFromJwtToken(String token) throws UserException {

        String email = jwtProvider.getEmailFromToken(token);
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new UserException("Invalid Token ");
        }

        return user;
    }

    @Override
    public User getCurrentUser() throws UserException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        if ( user == null){
            throw new UserException("User not found");
        }
        return user;
    }

    @Override
    public User getUserByEmail(String email) throws UserException {
        User user = userRepository.findByEmail(email);
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
    public List<User> getAllUser() {
        return userRepository.findAll();
    }
}
