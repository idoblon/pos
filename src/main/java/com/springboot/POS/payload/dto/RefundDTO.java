package com.springboot.POS.payload.dto;

import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.modal.ShiftReport;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class RefundDTO {

    private Long id;

    private Long orderId;

    private String reason;

    private Double amount;

    //private ShiftReport shiftReport;

    private Long shiftReportId;

    private UserDTO cashier;

    private String cashierName;

    private BranchDTO branch;

    private Long branchId;

    private PaymentType paymentType;

    private LocalDateTime createdAt;
}
