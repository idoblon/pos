package com.springboot.POS.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hash")
@CrossOrigin(origins = "http://localhost:3000")
public class HashController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/generate/{password}")
    public ResponseEntity<Map<String, String>> generatePasswordHash(@PathVariable String password) {
        try {
            String hashedPassword = passwordEncoder.encode(password);
            
            Map<String, String> response = new HashMap<>();
            response.put("originalPassword", password);
            response.put("hashedPassword", hashedPassword);
            response.put("encoderType", passwordEncoder.getClass().getSimpleName());
            
            // Log to console for easy copying
            System.out.println("=================================");
            System.out.println("PASSWORD HASH GENERATED");
            System.out.println("=================================");
            System.out.println("Original Password: " + password);
            System.out.println("Hashed Password: " + hashedPassword);
            System.out.println("Encoder Type: " + passwordEncoder.getClass().getSimpleName());
            System.out.println("=================================");
            System.out.println("SQL UPDATE COMMAND:");
            System.out.println("UPDATE pos.user SET password = '" + hashedPassword + "' WHERE email = 'posproofficial@gmail.com';");
            System.out.println("=================================");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate hash: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generatePasswordHashPost(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Password is required");
            return ResponseEntity.badRequest().body(error);
        }
        
        return generatePasswordHash(password);
    }

    @GetMapping("/test/{password}/{hash}")
    public ResponseEntity<Map<String, Object>> testPasswordMatch(
            @PathVariable String password, 
            @PathVariable String hash) {
        try {
            boolean matches = passwordEncoder.matches(password, hash);
            
            Map<String, Object> response = new HashMap<>();
            response.put("password", password);
            response.put("hash", hash);
            response.put("matches", matches);
            
            System.out.println("Password Test - Password: " + password);
            System.out.println("Password Test - Matches: " + matches);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to test password: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/admin-reset")
    public ResponseEntity<Map<String, String>> generateAdminHash() {
        String adminPassword = "Pos@123#!";
        String adminEmail = "posproofficial@gmail.com";
        
        try {
            String hashedPassword = passwordEncoder.encode(adminPassword);
            
            Map<String, String> response = new HashMap<>();
            response.put("email", adminEmail);
            response.put("originalPassword", adminPassword);
            response.put("hashedPassword", hashedPassword);
            response.put("sqlCommand", "UPDATE pos.user SET password = '" + hashedPassword + "' WHERE email = '" + adminEmail + "';");
            
            System.out.println("=================================");
            System.out.println("ADMIN PASSWORD RESET HASH");
            System.out.println("=================================");
            System.out.println("Admin Email: " + adminEmail);
            System.out.println("Admin Password: " + adminPassword);
            System.out.println("Generated Hash: " + hashedPassword);
            System.out.println("=================================");
            System.out.println("COPY AND RUN THIS SQL COMMAND:");
            System.out.println("UPDATE pos.user SET password = '" + hashedPassword + "' WHERE email = '" + adminEmail + "';");
            System.out.println("=================================");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate admin hash: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}