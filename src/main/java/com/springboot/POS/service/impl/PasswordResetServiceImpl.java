package com.springboot.POS.service.impl;

import com.springboot.POS.modal.User;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.EmailService;
import com.springboot.POS.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getDeleted())) {
                return;
            }

            String token = generateToken();
            user.setPasswordResetTokenHash(hash(token));
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);

            String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/reset-password")
                    .queryParam("token", token)
                    .build()
                    .toUriString();
            emailService.sendPasswordResetLink(user.getEmail(), user.getFullName(), resetUrl);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String tokenHash = hash(token);
        User user = userRepository.findByPasswordResetTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("This password reset link is invalid or has expired."));

        if (Boolean.TRUE.equals(user.getDeleted()) || user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            clearToken(user);
            throw new IllegalArgumentException("This password reset link is invalid or has expired.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        clearToken(user);
        userRepository.save(user);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private void clearToken(User user) {
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
    }
}
