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
    private Double taxAmount;
    private Double discount;
    private String discountType;
    private LocalDateTime createdAt;
    private Long branchId;
    private Long customerId;
    private BranchDTO branch;
    private UserDTO cashier;
    private Customer customer;
    private PaymentType paymentType;
    private String paymentMethod;    // frontend sends paymentMethod as string
    private String paymentReference; // eSewa ref / Khalti token / card last-4
    private Double amountReceived;   // CASH only
    private OrderStatus status;
    private List<OrderItemDTO> items;

    // Resolve paymentType from either paymentType or paymentMethod string
    public PaymentType getPaymentType() {
        if (paymentType != null) return paymentType;
        if (paymentMethod != null) {
            try { return PaymentType.valueOf(paymentMethod.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}
