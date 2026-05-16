package com.springboot.POS.payload.dto;

import com.springboot.POS.domain.OrderStatus;
import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.modal.Customer;
import com.springboot.POS.payload.dto.BranchDTO;
import com.springboot.POS.payload.dto.OrderItemDTO;
import com.springboot.POS.payload.dto.UserDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDTO {

    private Long id;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private Long branchId;
    private Long customerId;
    private BranchDTO branch;
    private UserDTO cashier;
    private Customer customer;
    private PaymentType paymentType;
    private OrderStatus status;
    private List<OrderItemDTO> items;
}
