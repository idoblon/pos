package com.springboot.POS.service.impl;

import com.springboot.POS.domain.OrderStatus;
import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.mapper.OrderMapper;
import com.springboot.POS.modal.*;
import com.springboot.POS.payload.dto.OrderDTO;
import com.springboot.POS.payload.dto.OrderItemDTO;
import com.springboot.POS.repository.CustomerRepository;
import com.springboot.POS.repository.OrderRepository;
import com.springboot.POS.repository.ProductRepository;
import com.springboot.POS.domain.UserRole;
import com.springboot.POS.repository.BranchRepository;
import com.springboot.POS.service.InventoryService;
import com.springboot.POS.service.OrderPaymentService;
import com.springboot.POS.service.OrderService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.util.BulkPricing;
import com.springboot.POS.util.JsonLists;
import com.springboot.POS.util.OrderTotals;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final BranchRepository branchRepository;

    /** VAT rate as a decimal fraction (0.13 = 13%). */
    @Value("${app.tax.rate:0.13}")
    private BigDecimal taxRate;

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
        if (branch == null && cashier.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            branch = branchRepository.findByManagerId(cashier.getId()).orElse(null);
        }
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

        // Pharmacy guardrails: prescription items need a recorded customer,
        // controlled substances need pharmacist sign-off — before any payment.
        if (orderItems.stream().anyMatch(item -> Boolean.TRUE.equals(item.getProduct().getPrescriptionRequired()))
                && orderDTO.getCustomerId() == null) {
            throw new Exception("Prescription items require a customer on the order");
        }
        if (orderItems.stream().anyMatch(item -> Boolean.TRUE.equals(item.getProduct().getControlledSubstance()))
                && !Boolean.TRUE.equals(orderDTO.getPrescriptionVerified())) {
            throw new Exception("Controlled substances require pharmacist verification");
        }

        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = OrderTotals.tax(subtotal, taxRate);
        BigDecimal discountAmount = OrderTotals.discountAmount(
                subtotal, orderDTO.getDiscount(), orderDTO.getDiscountType());
        BigDecimal finalTotal = OrderTotals.total(subtotal, taxAmount, discountAmount);

        // Resolve reference from either paymentReference or transactionId
        String paymentRef = orderDTO.getPaymentReference() != null
                ? orderDTO.getPaymentReference()
                : orderDTO.getTransactionId();

        // Verify payment with gateway / validate cash
        orderPaymentService.verify(
                paymentType,
                paymentRef,
                orderDTO.getAmountReceived() != null ? orderDTO.getAmountReceived().doubleValue() : null,
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
                .amountReceived(paymentType == PaymentType.CASH ? orderDTO.getAmountReceived() : finalTotal)
                .totalAmount(finalTotal)
                .taxAmount(taxAmount)
                .discount(orderDTO.getDiscount())
                .discountType(orderDTO.getDiscountType())
                .note(orderDTO.getNote())
                .idempotencyKey(scopedIdempotencyKey)
                .orderType(normalizeOrderType(orderDTO.getOrderType()))
                .tableNumber(blankToNull(orderDTO.getTableNumber()))
                .kitchenNote(blankToNull(orderDTO.getKitchenNote()))
                .prescriptionVerified(Boolean.TRUE.equals(orderDTO.getPrescriptionVerified()))
                .emiMonths(validateEmiMonths(orderDTO.getEmiMonths()))
                .status(OrderStatus.COMPLETED)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        for (OrderItem item : orderItems) {
            inventoryService.deductStock(item.getProduct().getId(), branch.getId(),
                    OrderTotals.stockUnits(item.getQuantity()));
        }

        Order savedOrder = orderRepository.save(order);
        return OrderMapper.toDTO(savedOrder);
    }

    /**
     * Lost the idempotency race against a concurrent identical request?
     * The unique idempotency_key constraint rejects the duplicate insert with
     * {@link DataIntegrityViolationException}; the winner row is then read in a
     * fresh transaction (this method is invoked from outside the failed one).
     */
    @Override
    public OrderDTO getOrderByIdempotencyKey(String rawKey) throws Exception {
        if (rawKey == null || rawKey.isBlank()) return null;
        User cashier = userService.getCurrentUser();
        String scoped = cashier.getId() + ":" + rawKey.trim();
        return orderRepository.findByIdempotencyKey(scoped).map(OrderMapper::toDTO).orElse(null);
    }

    @Override
    public List<OrderDTO> getAllOrders(int limit) {
        return orderRepository.findAll(PageRequest.of(0, com.springboot.POS.util.QueryLimits.clamp(limit),
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDTO holdOrder(OrderDTO orderDTO) throws Exception {
        User cashier = userService.getCurrentUser();
        Branch branch = cashier.getBranch();
        if (branch == null && cashier.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            branch = branchRepository.findByManagerId(cashier.getId()).orElse(null);
        }
        if (branch == null) throw new Exception("Cashier's branch not found");

        List<OrderItem> orderItems = buildOrderItems(orderDTO);
        BigDecimal subtotal = orderItems.stream().map(OrderItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Customer customer = orderDTO.getCustomerId() == null ? null
                : customerRepository.findById(orderDTO.getCustomerId()).orElse(null);
        PaymentType paymentType = orderDTO.getPaymentType() == null ? PaymentType.CASH : orderDTO.getPaymentType();
        Order heldOrder = Order.builder()
                .branch(branch)
                .cashier(cashier)
                .customer(customer)
                .paymentType(paymentType)
                .totalAmount(subtotal)
                .taxAmount(BigDecimal.ZERO)
                .discount(orderDTO.getDiscount())
                .discountType(orderDTO.getDiscountType())
                .note(orderDTO.getNote())
                .orderType(normalizeOrderType(orderDTO.getOrderType()))
                .tableNumber(blankToNull(orderDTO.getTableNumber()))
                .kitchenNote(blankToNull(orderDTO.getKitchenNote()))
                .prescriptionVerified(Boolean.TRUE.equals(orderDTO.getPrescriptionVerified()))
                .emiMonths(validateEmiMonths(orderDTO.getEmiMonths()))
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
        if (branch == null && cashier.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            branch = branchRepository.findByManagerId(cashier.getId()).orElse(null);
        }
        if (branch == null) throw new Exception("Cashier's branch not found");
        return orderRepository.findByCashierIdAndBranchIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
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
        // DELETE is intentionally idempotent: a double click or a delayed retry
        // must not turn a successfully removed held order into a server error.
        Order existingOrder = orderRepository.findById(id).orElse(null);
        if (existingOrder == null || Boolean.TRUE.equals(existingOrder.getDeleted())) {
            return;
        }
        Order order = findOwnedHeldOrder(id);
        // Do not change the status or physically delete the order. Existing
        // installations can have legacy enum constraints or foreign keys; the
        // model's deleted flag provides a safe, reversible discard operation.
        order.setDeleted(true);
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

    @Override
    public List<OrderDTO> getOrdersByStore(Long storeId) throws Exception {
        return orderRepository.findByStoreId(storeId).stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getMonthlyOrdersByBranch(Long branchId) throws Exception {
        LocalDateTime from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();
        return orderRepository.findCompletedByBranchIdAndDateRange(branchId, from, to)
                .stream().map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getMonthlyOrdersByStore(Long storeId) throws Exception {
        LocalDateTime from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();
        return orderRepository.findCompletedByStoreIdAndDateRange(storeId, from, to)
                .stream().map(OrderMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * Builds priced order lines from the request. Lines carrying distinguishing
     * attributes (modifiers, kitchen note, dosage, serials) are kept separate
     * so per-line detail survives; plain lines of the same product share the
     * bulk tier reached by the product's total quantity.
     *
     * Enforced here (both new sales and held orders): products exist and are
     * not deleted, quantities are positive, expired goods are rejected, MOQ is
     * met per product, and serialized goods carry one unique serial per unit.
     */
    private List<OrderItem> buildOrderItems(OrderDTO orderDTO) {
        if (orderDTO.getItems() == null || orderDTO.getItems().isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one item");
        }
        List<ResolvedLine> lines = new ArrayList<>();
        Map<Long, BigDecimal> totalsByProduct = new TreeMap<>();
        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            if (itemDTO == null || itemDTO.getProductId() == null) {
                throw new IllegalArgumentException("Every order item must include a product");
            }
            if (itemDTO.getQuantity() == null || itemDTO.getQuantity().signum() <= 0) {
                throw new IllegalArgumentException("Order item quantity must be greater than zero");
            }
            BigDecimal qty;
            try {
                qty = itemDTO.getQuantity().stripTrailingZeros();
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("Order item quantity is too large", ex);
            }
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: id=" + itemDTO.getProductId()));
            if (Boolean.TRUE.equals(product.getDeleted())) {
                throw new EntityNotFoundException("Product no longer available: " + product.getName());
            }
            if (product.getExpiryDate() != null && !product.getExpiryDate().isAfter(java.time.LocalDate.now())) {
                throw new IllegalArgumentException("Product expired and cannot be sold: " + product.getName());
            }
            if (product.getSellingPrice() == null || product.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Product has an invalid selling price: " + product.getId());
            }
            List<String> modifiers = cleanStrings(itemDTO.getModifiers());
            List<String> serials = cleanStrings(itemDTO.getSerials());
            lines.add(new ResolvedLine(product, qty, modifiers,
                    blankToNull(itemDTO.getKitchenNote()), blankToNull(itemDTO.getDosage()), serials));
            totalsByProduct.merge(product.getId(), qty, BigDecimal::add);
        }
        // MOQ is measured against the product's total across all lines.
        for (ResolvedLine line : lines) {
            Integer moq = line.product.getMoq();
            if (moq != null && moq > 1
                    && totalsByProduct.getOrDefault(line.product.getId(), BigDecimal.ZERO)
                            .compareTo(BigDecimal.valueOf(moq)) < 0) {
                throw new IllegalArgumentException(
                        "Minimum order quantity of " + moq + " not met for: " + line.product.getName());
            }
        }
        // Serials: one unique non-blank serial per whole unit, per product.
        Map<Long, java.util.Set<String>> seenSerials = new java.util.HashMap<>();
        for (ResolvedLine line : lines) {
            if (Boolean.TRUE.equals(line.product.getRequiresSerial())) {
                int units;
                try {
                    units = line.qty.intValueExact();
                } catch (ArithmeticException ex) {
                    throw new IllegalArgumentException(
                            "Serialized product requires a whole-unit quantity: " + line.product.getName());
                }
                if (line.serials.size() != units) {
                    throw new IllegalArgumentException(
                            "One unique serial per unit is required for: " + line.product.getName());
                }
                java.util.Set<String> seen = seenSerials.computeIfAbsent(
                        line.product.getId(), k -> new java.util.HashSet<>());
                for (String serial : line.serials) {
                    if (!seen.add(serial)) {
                        throw new IllegalArgumentException(
                                "Duplicate serial for " + line.product.getName() + ": " + serial);
                    }
                }
            }
        }
        return lines.stream().map(line -> {
            Product product = line.product;
            BigDecimal productTotal = totalsByProduct.getOrDefault(product.getId(), line.qty);
            BigDecimal unitPrice = BulkPricing.effectiveUnitPrice(
                    product.getSellingPrice(), product.getBulkTiersJson(),
                    product.getBulkMinQty(), product.getBulkPrice(), productTotal);
            return OrderItem.builder()
                    .product(product)
                    .quantity(line.qty)
                    .unitPrice(unitPrice)
                    .price(unitPrice.multiply(line.qty).setScale(2, RoundingMode.HALF_UP))
                    .modifiersJson(line.modifiers.isEmpty() ? null : JsonLists.toJson(line.modifiers))
                    .kitchenNote(line.kitchenNote)
                    .dosage(line.dosage)
                    .serialsJson(line.serials.isEmpty() ? null : JsonLists.toJson(line.serials))
                    .build();
        }).collect(Collectors.toList());
    }

    /** Validated line carried from the request DTO into a priced entity. */
    private static final class ResolvedLine {
        private final Product product;
        private final BigDecimal qty;
        private final List<String> modifiers;
        private final String kitchenNote;
        private final String dosage;
        private final List<String> serials;

        private ResolvedLine(Product product, BigDecimal qty, List<String> modifiers,
                             String kitchenNote, String dosage, List<String> serials) {
            this.product = product;
            this.qty = qty;
            this.modifiers = modifiers;
            this.kitchenNote = kitchenNote;
            this.dosage = dosage;
            this.serials = serials;
        }
    }

    private static List<String> cleanStrings(List<String> values) {
        if (values == null) return new ArrayList<>();
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Accepts DINE_IN / TAKEAWAY / DELIVERY (case-insensitive) or null. */
    private static String normalizeOrderType(String orderType) {
        if (orderType == null || orderType.isBlank()) return null;
        String normalized = orderType.trim().toUpperCase();
        if (!normalized.equals("DINE_IN") && !normalized.equals("TAKEAWAY") && !normalized.equals("DELIVERY")) {
            throw new IllegalArgumentException("Unknown order type: " + orderType);
        }
        return normalized;
    }

    /** EMI tenure travels with the order for records; the gateway still charges in full. */
    private static Integer validateEmiMonths(Integer emiMonths) {
        if (emiMonths == null) return null;
        if (emiMonths <= 0) throw new IllegalArgumentException("EMI tenure must be positive");
        return emiMonths;
    }

    private Order findOwnedHeldOrder(Long id) throws Exception {
        User cashier = userService.getCurrentUser();
        Branch branch = cashier.getBranch();
        if (branch == null && cashier.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            branch = branchRepository.findByManagerId(cashier.getId()).orElse(null);
        }
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Held order not found"));
        if (order.getStatus() != OrderStatus.HELD || order.getCashier() == null
                || !order.getCashier().getId().equals(cashier.getId()) || branch == null
                || order.getBranch() == null || !order.getBranch().getId().equals(branch.getId())) {
            throw new IllegalAccessException("You can only manage your own held orders");
        }
        return order;
    }
}
