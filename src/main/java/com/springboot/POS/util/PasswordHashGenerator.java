package com.springboot.POS.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "Pos@123#!";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Raw Password: " + rawPassword);
        System.out.println("Encoded Password: " + encodedPassword);
        
        // Verify the password works
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("Password verification: " + matches);
    }
}