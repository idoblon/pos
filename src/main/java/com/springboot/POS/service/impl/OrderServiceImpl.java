package com.springboot.POS.service.impl;

import com.springboot.POS.domain.OrderStatus;
import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.mapper.OrderMapper;
import com.springboot.POS.modal.*;
import com.springboot.POS.payload.dto.OrderDTO;
import com.springboot.POS.repository.OrderItemRepository;
import com.springboot.POS.repository.OrderRepository;
import com.springboot.POS.repository.ProductRepository;
import com.springboot.POS.service.OrderService;
import com.springboot.POS.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final UserService userService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) throws Exception {
        User cashier = userService.getCurrentUser();

        Branch branch = cashier.getBranch();
        if(branch == null){
            throw new Exception("cashier's branch not found");
        }
        Order order = Order.builder()
                .branch(branch)
                .cashier(cashier)
                .customer(orderDTO.getCustomer())
                .paymentType(orderDTO.getPaymentType())
                .build();


        List<OrderItem> orderItems = orderDTO.getItems().stream().map(
                itemDTO -> {
                    Product product = productRepository.findById(itemDTO.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException("product not found"));

                    return OrderItem.builder()
                            .product(product)
                            .quantity(itemDTO.getQuantity())
                            .price(product.getSellingPrice()* itemDTO.getQuantity())
                            .order(order)
                            .build();

                }
        ).toList();
        double total = orderItems.stream().mapToDouble(
                OrderItem::getPrice
        ).sum();
        order.setTotalAmount(total);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);
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
                .filter(order -> customerId==null ||
                        (order.getCustomer()!=null &&
                                order.getCustomer().getId().equals(customerId)))
                .filter(order -> cashierId==null ||
                        order.getCashier()!=null &&
                        order.getCashier().getId().equals(cashierId))
                .filter(order -> paymentType == null ||
                        order.getPaymentType()==paymentType)
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
