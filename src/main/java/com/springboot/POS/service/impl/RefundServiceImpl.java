package com.springboot.POS.service.impl;

import com.springboot.POS.mapper.RefundMapper;
import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.Order;
import com.springboot.POS.modal.Refund;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.RefundDTO;
import com.springboot.POS.repository.OrderRepository;
import com.springboot.POS.repository.RefundRepository;
import com.springboot.POS.service.RefundService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;

    @Override
    public RefundDTO createRefund(RefundDTO refund) throws Exception {
        User cashier = userService.getCurrentUser();
        Order order = orderRepository.findById(refund.getOrderId()).orElseThrow(
                () -> new Exception("Order not found")
        );

        // Validate refund amount
        if (refund.getAmount() == null || refund.getAmount() <= 0) {
            throw new Exception("Refund amount must be greater than zero");
        }
        if (refund.getAmount() > order.getTotalAmount()) {
            throw new Exception("Refund amount (" + refund.getAmount() + ") cannot exceed order total (" + order.getTotalAmount() + ")");
        }

        // Check if order was already fully refunded
        List<Refund> existingRefunds = refundRepository.findByOrderId(order.getId());
        double totalRefunded = existingRefunds.stream().mapToDouble(Refund::getAmount).sum();
        if (totalRefunded + refund.getAmount() > order.getTotalAmount()) {
            throw new Exception("Total refunds would exceed order amount. Already refunded: " + totalRefunded);
        }

        Branch branch = order.getBranch();

        Refund createdRefund = Refund.builder()
                .order(order)
                .cashier(cashier)
                .branch(branch)
                .reason(refund.getReason())
                .amount(refund.getAmount())
                .createdAt(refund.getCreatedAt())
                .build();

        Refund savedRefund = refundRepository.save(createdRefund);
        return RefundMapper.toDTO(savedRefund);
    }

    @Override
    public List<RefundDTO> getAllRefunds() throws Exception {

        return refundRepository.findAll().stream().map(
                RefundMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<RefundDTO> getRefundByCashier(Long cashierId) throws Exception {
        return refundRepository.findByCashierId(cashierId).stream()
                        .map(RefundMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<RefundDTO> getRefundByShiftReport(Long shiftReportId) throws Exception {
        return refundRepository.findByShiftReportId(shiftReportId).stream().map(
                RefundMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<RefundDTO> getRefundByCashierIdAndDateRange(Long cashierId,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate) throws Exception {
        return refundRepository.findByCashierIdAndCreatedAtBetween(
                cashierId, startDate, endDate
        ).stream().map(RefundMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<RefundDTO> getRefundByBranch(Long branchId) throws Exception {
        return refundRepository.findByBranchId(branchId).stream().map(
                RefundMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public RefundDTO getRefundById(Long refundId) throws Exception {
        return refundRepository.findById(refundId).map(
                RefundMapper::toDTO).orElseThrow(
                () -> new Exception("Refund not found")
        );
    }

    @Override
    public void deleteRefund(Long refundId) throws Exception {
        this.getRefundById(refundId);
        refundRepository.deleteById(refundId);

    }
}
