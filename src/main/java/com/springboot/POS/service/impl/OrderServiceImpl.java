package com.springboot.POS.service.impl;

import com.springboot.POS.domain.OrderStatus;
import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.mapper.OrderMapper;
import com.springboot.POS.modal.*;
import com.springboot.POS.payload.dto.OrderDTO;
import com.springboot.POS.repository.CustomerRepository;
import com.springboot.POS.repository.OrderRepository;
import com.springboot.POS.repository.ProductRepository;
import com.springboot.POS.service.InventoryService;
import com.springboot.POS.service.OrderPaymentService;
import com.springboot.POS.service.OrderService;
import com.springboot.POS.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final UserService userService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final CustomerRepository customerRepository;
    private final OrderPaymentService orderPaymentService;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) throws Exception {
        User cashier = userService.getCurrentUser();

        Branch branch = cashier.getBranch();
        if (branch == null) {
            throw new Exception("Cashier's branch not found");
        }

        // Resolve payment type (frontend may send paymentMethod string or paymentType enum)
        PaymentType paymentType = orderDTO.getPaymentType();
        if (paymentType == null) {
            throw new Exception("Payment method is required");
        }

        Long storeId = branch.getStore() != null ? branch.getStore().getId() : null;

        // ── Payment verification BEFORE touching any DB records ───────────────
        if (storeId != null && !orderPaymentService.isPaymentMethodEnabled(storeId, paymentType)) {
            throw new Exception(paymentType + " payment is not enabled for this store.");
        }

        // Build order items first so we know the real total
        List<OrderItem> orderItems = orderDTO.getItems().stream().map(itemDTO -> {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Product not found: id=" + itemDTO.getProductId()));

            if (Boolean.TRUE.equals(product.getDeleted())) {
                throw new EntityNotFoundException("Product no longer available: " + product.getName());
            }

            double unitPrice = product.getSellingPrice();
            return OrderItem.builder()
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .unitPrice(unitPrice)
                    .price(unitPrice * itemDTO.getQuantity())
                    .build();
        }).collect(Collectors.toList());

        double subtotal = orderItems.stream().mapToDouble(OrderItem::getPrice).sum();
        double taxAmount = subtotal * 0.13;

        double discountAmount = 0.0;
        if (orderDTO.getDiscount() != null && orderDTO.getDiscount() > 0) {
            discountAmount = "percentage".equalsIgnoreCase(orderDTO.getDiscountType())
                    ? subtotal * (orderDTO.getDiscount() / 100)
                    : orderDTO.getDiscount();
        }

        double finalTotal = subtotal + taxAmount - discountAmount;

        // ── Verify payment with gateway / validate cash ───────────────────────
        orderPaymentService.verify(
                paymentType,
                orderDTO.getPaymentReference(),
                orderDTO.getAmountReceived(),
                finalTotal,
                storeId
        );

        // ── Resolve customer ──────────────────────────────────────────────────
        Customer customer = null;
        if (orderDTO.getCustomerId() != null) {
            customer = customerRepository.findById(orderDTO.getCustomerId()).orElse(null);
        }

        // ── Persist order ─────────────────────────────────────────────────────
        Order order = Order.builder()
                .branch(branch)
                .cashier(cashier)
                .customer(customer)
                .paymentType(paymentType)
                .paymentReference(orderDTO.getPaymentReference())
                .amountReceived(paymentType == PaymentType.CASH ? orderDTO.getAmountReceived() : finalTotal)
                .totalAmount(finalTotal)
                .taxAmount(taxAmount)
                .discount(orderDTO.getDiscount())
                .discountType(orderDTO.getDiscountType())
                .status(OrderStatus.COMPLETED)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // ── Deduct inventory after successful save ────────────────────────────
        for (OrderItem item : orderItems) {
            inventoryService.deductStock(item.getProduct().getId(), branch.getId(), item.getQuantity());
        }

        return OrderMapper.toDTO(savedOrder);
    }

    @Override
    public OrderDTO getOrderById(Long id) throws Exception {
        return orderRepository.findById(id)
                .map(OrderMapper::toDTO)
                .orElseThrow(
                ()-> new Exception("order not found with id" + id)
        );
    }

    @Override
    public List<OrderDTO> getOrdersByBranch(Long branchId,
                                            Long customerId,
                                            Long cashierId,
                                            PaymentType paymentType,
                                            OrderStatus status) throws Exception {
        return orderRepository.findByBranchId(branchId).stream()
                .filter(order -> customerId == null ||
                        (order.getCustomer() != null &&
                                order.getCustomer().getId().equals(customerId)))
                .filter(order -> cashierId == null ||
                        order.getCashier() != null &&
                        order.getCashier().getId().equals(cashierId))
                .filter(order -> paymentType == null ||
                        order.getPaymentType() == paymentType)
                .filter(order -> status == null ||
                        order.getStatus() == status)
                .map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getOrderByCashier(Long cashierId) {
        return orderRepository.findByCashierId(cashierId).stream()
                .map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteOrder(Long id) throws Exception {
        Order order = orderRepository.findById(id).orElseThrow(
                () -> new Exception("order not found with id " + id)
        );
        orderRepository.delete(order);

    }

    @Override
    public List<OrderDTO> getTodayOrdersByBranch(Long branchId) throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();


        return orderRepository.findByBranchIdAndCreatedAtBetween(
                branchId, start, end
        ).stream().map(
                OrderMapper::toDTO
        ).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getOrdersByCustomerId(Long customerId) throws Exception {
        return orderRepository.findByCustomerId(customerId).stream().map(
                OrderMapper::toDTO
        ).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getTop5RecentOrdersByBranchId(Long branchId) throws Exception {
        return orderRepository.findByBranchIdOrderByCreatedAtDesc(branchId).stream().map(
                OrderMapper::toDTO
        ).collect(Collectors.toList());
    }
}
