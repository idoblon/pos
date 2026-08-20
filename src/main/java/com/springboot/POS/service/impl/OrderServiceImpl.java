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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    public OrderDTO createOrder(OrderDTO orderDTO, String idempotencyKey) throws Exception {
        User cashier = userService.getCurrentUser();

        String scopedIdempotencyKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? null
                : cashier.getId() + ":" + idempotencyKey.trim();
        if (scopedIdempotencyKey != null) {
            Order existing = orderRepository.findByIdempotencyKey(scopedIdempotencyKey).orElse(null);
            if (existing != null) return OrderMapper.toDTO(existing);
        }

        Branch branch = cashier.getBranch();
        if (branch == null) {
            throw new Exception("Cashier's branch not found");
        }

        PaymentType paymentType = orderDTO.getPaymentType();
        if (paymentType == null) {
            throw new Exception("Payment method is required");
        }

        Long storeId = branch.getStore() != null ? branch.getStore().getId() : null;

        if (storeId != null && !orderPaymentService.isPaymentMethodEnabled(storeId, paymentType)) {
            throw new Exception(paymentType + " payment is not enabled for this store.");
        }

        List<OrderItem> orderItems = buildOrderItems(orderDTO);

        BigDecimal subtotal = orderItems.stream()
                .map(item -> BigDecimal.valueOf(item.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.13"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (orderDTO.getDiscount() != null) {
            if (!Double.isFinite(orderDTO.getDiscount()) || orderDTO.getDiscount() < 0) {
                throw new IllegalArgumentException("Discount must be a non-negative number");
            }
            BigDecimal discount = BigDecimal.valueOf(orderDTO.getDiscount());
            if ("percentage".equalsIgnoreCase(orderDTO.getDiscountType())) {
                if (discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("Percentage discount cannot exceed 100");
                }
                discountAmount = subtotal.multiply(discount)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                if (discount.compareTo(subtotal) > 0) {
                    throw new IllegalArgumentException("Discount cannot exceed the order subtotal");
                }
                discountAmount = discount.setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal finalTotal = subtotal.add(taxAmount).subtract(discountAmount);

        // Resolve reference from either paymentReference or transactionId
        String paymentRef = orderDTO.getPaymentReference() != null
                ? orderDTO.getPaymentReference()
                : orderDTO.getTransactionId();

        // Verify payment with gateway / validate cash
        orderPaymentService.verify(
                paymentType,
                paymentRef,
                orderDTO.getAmountReceived(),
                finalTotal.doubleValue(),
                storeId
        );

        // Resolve customer
        Customer customer = null;
        if (orderDTO.getCustomerId() != null) {
            customer = customerRepository.findById(orderDTO.getCustomerId()).orElse(null);
        }

        // Persist order
        Order order = Order.builder()
                .branch(branch)
                .cashier(cashier)
                .customer(customer)
                .paymentType(paymentType)
                .paymentReference(paymentRef)
                .amountReceived(paymentType == PaymentType.CASH ? orderDTO.getAmountReceived() : finalTotal.doubleValue())
                .totalAmount(finalTotal.doubleValue())
                .taxAmount(taxAmount.doubleValue())
                .discount(orderDTO.getDiscount())
                .discountType(orderDTO.getDiscountType())
                .note(orderDTO.getNote())
                .idempotencyKey(scopedIdempotencyKey)
                .status(OrderStatus.COMPLETED)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        for (OrderItem item : orderItems) {
            inventoryService.deductStock(item.getProduct().getId(), branch.getId(), item.getQuantity());
        }

        Order savedOrder = orderRepository.save(order);
        return OrderMapper.toDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO holdOrder(OrderDTO orderDTO) throws Exception {
        User cashier = userService.getCurrentUser();
        Branch branch = cashier.getBranch();
        if (branch == null) throw new Exception("Cashier's branch not found");

        List<OrderItem> orderItems = buildOrderItems(orderDTO);
        BigDecimal subtotal = orderItems.stream().map(item -> BigDecimal.valueOf(item.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Customer customer = orderDTO.getCustomerId() == null ? null
                : customerRepository.findById(orderDTO.getCustomerId()).orElse(null);
        PaymentType paymentType = orderDTO.getPaymentType() == null ? PaymentType.CASH : orderDTO.getPaymentType();
        Order heldOrder = Order.builder()
                .branch(branch)
                .cashier(cashier)
                .customer(customer)
                .paymentType(paymentType)
                .totalAmount(subtotal.doubleValue())
                .taxAmount(0D)
                .discount(orderDTO.getDiscount())
                .discountType(orderDTO.getDiscountType())
                .note(orderDTO.getNote())
                .status(OrderStatus.HELD)
                .build();
        orderItems.forEach(item -> item.setOrder(heldOrder));
        heldOrder.setItems(orderItems);
        return OrderMapper.toDTO(orderRepository.save(heldOrder));
    }

    @Override
    public List<OrderDTO> getHeldOrders() throws Exception {
        User cashier = userService.getCurrentUser();
        Branch branch = cashier.getBranch();
        if (branch == null) throw new Exception("Cashier's branch not found");
        return orderRepository.findByCashierIdAndBranchIdAndStatusOrderByCreatedAtDesc(
                        cashier.getId(), branch.getId(), OrderStatus.HELD)
                .stream().map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDTO resumeHeldOrder(Long id) throws Exception {
        Order order = findOwnedHeldOrder(id);
        order.setStatus(OrderStatus.RESUMED);
        return OrderMapper.toDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void discardHeldOrder(Long id) throws Exception {
        Order order = findOwnedHeldOrder(id);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    public OrderDTO getOrderById(Long id) throws Exception {
        return orderRepository.findById(id)
                .map(OrderMapper::toDTO)
                .orElseThrow(() -> new Exception("order not found with id" + id));
    }

    @Override
    public List<OrderDTO> getOrdersByBranch(Long branchId, Long customerId, Long cashierId,
                                            PaymentType paymentType, OrderStatus status) throws Exception {
        return orderRepository.findByBranchId(branchId).stream()
                .filter(order -> customerId == null ||
                        (order.getCustomer() != null && order.getCustomer().getId().equals(customerId)))
                .filter(order -> cashierId == null ||
                        order.getCashier() != null && order.getCashier().getId().equals(cashierId))
                .filter(order -> paymentType == null || order.getPaymentType() == paymentType)
                .filter(order -> status == null ? order.getStatus() == OrderStatus.COMPLETED : order.getStatus() == status)
                .map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getOrderByCashier(Long cashierId) {
        return orderRepository.findByCashierId(cashierId).stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteOrder(Long id) throws Exception {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new Exception("order not found with id " + id));
        orderRepository.delete(order);
    }

    @Override
    public List<OrderDTO> getTodayOrdersByBranch(Long branchId) throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return orderRepository.findByBranchIdAndCreatedAtBetween(branchId, start, end)
                .stream().filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getOrdersByCustomerId(Long customerId) throws Exception {
        return orderRepository.findByCustomerId(customerId).stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getTop5RecentOrdersByBranchId(Long branchId) throws Exception {
        return orderRepository.findByBranchIdOrderByCreatedAtDesc(branchId).stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    private List<OrderItem> buildOrderItems(OrderDTO orderDTO) {
        Map<Long, Integer> quantitiesByProduct = new TreeMap<>();
        if (orderDTO.getItems() == null || orderDTO.getItems().isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one item");
        }
        orderDTO.getItems().forEach(itemDTO -> {
            if (itemDTO == null || itemDTO.getProductId() == null) {
                throw new IllegalArgumentException("Every order item must include a product");
            }
            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new IllegalArgumentException("Order item quantity must be greater than zero");
            }
            try {
                quantitiesByProduct.merge(itemDTO.getProductId(), itemDTO.getQuantity(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("Order item quantity is too large", ex);
            }
        });
        return quantitiesByProduct.entrySet().stream().map(entry -> {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: id=" + entry.getKey()));
            if (Boolean.TRUE.equals(product.getDeleted())) {
                throw new EntityNotFoundException("Product no longer available: " + product.getName());
            }
            if (product.getSellingPrice() == null || product.getSellingPrice() < 0) {
                throw new IllegalStateException("Product has an invalid selling price: " + product.getId());
            }
            double unitPrice = product.getSellingPrice();
            return OrderItem.builder().product(product).quantity(entry.getValue()).unitPrice(unitPrice)
                    .price(BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(entry.getValue()))
                            .setScale(2, RoundingMode.HALF_UP).doubleValue())
                    .build();
        }).collect(Collectors.toList());
    }

    private Order findOwnedHeldOrder(Long id) throws Exception {
        User cashier = userService.getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Held order not found"));
        if (order.getStatus() != OrderStatus.HELD || order.getCashier() == null
                || !order.getCashier().getId().equals(cashier.getId()) || cashier.getBranch() == null
                || order.getBranch() == null || !order.getBranch().getId().equals(cashier.getBranch().getId())) {
            throw new IllegalAccessException("You can only manage your own held orders");
        }
        return order;
    }
}
