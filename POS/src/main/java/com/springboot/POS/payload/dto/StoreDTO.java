package com.springboot.POS.payload.dto;

import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.modal.StoreContact;
import com.springboot.POS.modal.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoreDTO {

    private Long id;

    private String brand;

    private UserDto storeAdmin;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String description;

    private String storeType;

    private StoreStatus status;

    private StoreContact contact = new StoreContact();

}
