package com.springboot.POS.service;

import com.springboot.POS.domain.OrderStatus;
import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.payload.dto.OrderDTO;
import org.springframework.stereotype.Service;

import java.util.List;

public interface OrderService {

    OrderDTO createOrder(OrderDTO orderDTO, String idempotencyKey) throws Exception;
    /** Most recent orders across all tenants (admin scope), newest first, bounded. */
    List<OrderDTO> getAllOrders(int limit);
    OrderDTO holdOrder(OrderDTO orderDTO) throws Exception;
    List<OrderDTO> getHeldOrders() throws Exception;
    OrderDTO resumeHeldOrder(Long id) throws Exception;
    void discardHeldOrder(Long id) throws Exception;
    OrderDTO getOrderById(Long id) throws Exception;
    /** Winner of a lost idempotency race, or null when not found. */
    OrderDTO getOrderByIdempotencyKey(String rawKey) throws Exception;
    List<OrderDTO> getOrdersByBranch(Long branchId,
                                     Long customerId,
                                     Long cashierId,
                                     PaymentType paymentType,
                                     OrderStatus status) throws Exception;
    List<OrderDTO> getOrderByCashier(Long cashierId);
    void deleteOrder(Long id) throws Exception;
    List<OrderDTO> getOrdersByCustomerId(Long customerId) throws Exception;
    List<OrderDTO> getTodayOrdersByBranch(Long branchId) throws Exception;
    List<OrderDTO> getTop5RecentOrdersByBranchId(Long branchId) throws Exception;
    List<OrderDTO> getOrdersByStore(Long storeId) throws Exception;
    List<OrderDTO> getMonthlyOrdersByBranch(Long branchId) throws Exception;
    List<OrderDTO> getMonthlyOrdersByStore(Long storeId) throws Exception;
}
