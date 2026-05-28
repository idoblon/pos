package com.springboot.POS.payload.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailRequest {
    private String to;
    private String subject;
    private String userName;
    private String storeName;
    private String branchName;
    private String role;
    private String password;
    private Double amount;
    private String orderNumber;
}
