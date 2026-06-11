package com.springboot.POS.modal;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreContact {

    @Column(name = "contact_address")
    private String address;
    
    @Column(name = "contact_phone")
    private String phone;

    @Email(message = "Invalid email format")
    @Column(name = "contact_email")
    private String email;
}
