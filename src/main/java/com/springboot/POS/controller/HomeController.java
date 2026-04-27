package com.springboot.POS.controller;

import com.springboot.POS.payload.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeController {
    
    @GetMapping("/")
    public ResponseEntity<ApiResponse> home(){
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Welcome to POS System");
        return ResponseEntity.ok(apiResponse);
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> healthCheck(){
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("POS System is running successfully");
        return ResponseEntity.ok(apiResponse);
    }
}
