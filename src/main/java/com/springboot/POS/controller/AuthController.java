package com.springboot.POS.controller;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.payload.dto.UserDTO;
import com.springboot.POS.payload.response.AuthResponse;
import com.springboot.POS.service.AuthService;
import com.springboot.POS.service.PasswordResetService;
import com.springboot.POS.payload.dto.ForgotPasswordRequest;
import com.springboot.POS.payload.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

//    http://localhost:8080/auth/singup

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signupHandler(
            @RequestBody @Valid UserDTO userDto
    ) throws UserException {
        return ResponseEntity.ok(
                authService.signup(userDto)
        );

    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginHandler(
            @RequestBody UserDTO userDto
    ) throws UserException {
        return ResponseEntity.ok(
                authService.login(userDto)
        );

    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshTokenHandler(
            @RequestHeader("Authorization") String jwt
    ) throws UserException {
        return ResponseEntity.ok(
                authService.refreshToken(jwt)
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPasswordHandler(
            @RequestBody @Valid ForgotPasswordRequest request
    ) {
        passwordResetService.requestPasswordReset(request.getEmail());
        // Always return the same response so this endpoint cannot reveal which emails are registered.
        return ResponseEntity.ok(Map.of("message", "If that account exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPasswordHandler(
            @RequestBody @Valid ResetPasswordRequest request
    ) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Your password has been reset. You can now sign in."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
