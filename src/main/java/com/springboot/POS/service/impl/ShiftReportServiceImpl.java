package com.springboot.POS.service.impl;

import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.ShiftReportMapper;
import com.springboot.POS.modal.*;
import com.springboot.POS.payload.dto.ShiftReportDTO;
import com.springboot.POS.repository.*;
import com.springboot.POS.service.ShiftReportService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.hibernate.loader.LoaderLogging;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftReportServiceImpl implements ShiftReportService {


    private final ShiftReportRepository shiftReportRepository;
    private final UserService userService;
    private final BranchRepository branchRepository;
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;


    @Override
    public ShiftReportDTO startShift() throws Exception {
        User currentUser = userService.getCurrentUser();
        LocalDateTime shiftStart = LocalDateTime.now();

        LocalDateTime startOfDay = shiftStart.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = shiftStart.withHour(23).withMinute(59).withSecond(59);

        Optional<ShiftReport> existing = shiftReportRepository.findByCashierAndShiftStartBetween(
                currentUser, startOfDay, endOfDay
        );
        if(existing.isPresent()){
            throw new Exception("Shift already started today");
        }
        Branch branch = currentUser.getBranch();

        ShiftReport shiftReport = ShiftReport.builder()
                .cashier(currentUser)
                .shiftStart(shiftStart)
                .branch(branch)
                .build();
        ShiftReport savedReport = shiftReportRepository.save(shiftReport);
        return ShiftReportMapper.toDTO(savedReport);
    }

    @Override
    public ShiftReportDTO endShift(Long shiftReportId, LocalDateTime shiftEnd) throws Exception {
        User currentUser = userService.getCurrentUser();

        ShiftReport shiftReport = shiftReportRepository
                .findTopByCashierAndShiftEndIsNullOrderByShiftStartDesc(currentUser)
                .orElseThrow(() -> new Exception("Shift not found"));

        shiftReport.setShiftEnd(shiftEnd);

        List<Refund> refunds = refundRepository.findByCashierIdAndCreatedAtBetween(
                currentUser.getId(),
                shiftReport.getShiftStart(), shiftReport.getShiftEnd()
        );

        double totalRefunds = refunds.stream()
                .mapToDouble(refund -> refund.getAmount()!=null?
                        refund.getAmount():0.0).sum();

        List<Order> orders = orderRepository.findByCashierAndCreatedAtBetween(
                currentUser, shiftReport.getShiftStart(), shiftReport.getShiftEnd()
        );

        double totalSales = orders.stream()
                .mapToDouble(Order :: getTotalAmount).sum();

        int totalOrders = orders.size();

        double netSales = totalSales-totalRefunds;

        shiftReport.setTotalRefunds(totalRefunds);
        shiftReport.setTotalSales(totalSales);
        shiftReport.setTotalOrders(totalOrders);
        shiftReport.setNetSale(netSales);
        shiftReport.setRecentOrders(getRecentOrders(orders));
        shiftReport.setTopSellingProducts(getTopSellingProducts(orders));
        shiftReport.setPaymentSummaries(getPaymentSummaries(orders, totalSales));
        shiftReport.setRefunds(refunds);
        
        ShiftReport savedReport = shiftReportRepository.save(shiftReport);

        return ShiftReportMapper.toDTO(savedReport);
    }


    @Override
    public ShiftReportDTO getShiftReportById(Long id) throws Exception {
        return shiftReportRepository.findById(id).map(ShiftReportMapper::toDTO)
                .orElseThrow(
                ()-> new Exception("Shift report not found with given id" + id)
        );
    }

    @Override
    public List<ShiftReportDTO> getAllShiftReports() {
        List<ShiftReport> reports = shiftReportRepository.findAll();
        return reports.stream().map(
                ShiftReportMapper::toDTO
        ).collect(Collectors.toList());
    }

    @Override
    public List<ShiftReportDTO> getShiftReportByBranchId(Long branchId) {
        List<ShiftReport> reports = shiftReportRepository.findByBranchId(branchId);
        return reports.stream().map(
                ShiftReportMapper::toDTO
        ).collect(Collectors.toList());
    }

    @Override
    public List<ShiftReportDTO> getShiftReportByCashierId(Long cashierId) {
        List<ShiftReport> reports = shiftReportRepository.findByCashierId(cashierId);
        return reports.stream().map(
                ShiftReportMapper::toDTO
        ).collect(Collectors.toList());
    }

    @Override
    public ShiftReportDTO getCurrentShiftProgress(Long cashierId) throws Exception {
        User user = userService.getCurrentUser();

        ShiftReport shiftReport = shiftReportRepository.
                findTopByCashierAndShiftEndIsNullOrderByShiftStartDesc(user).orElseThrow(
                        ()-> new Exception("No active shift found for cashier"));

        LocalDateTime now = LocalDateTime.now();

        List<Order> orders = orderRepository.findByCashierAndCreatedAtBetween(
                user, shiftReport.getShiftStart(), now
        );

        List<Refund> refunds = refundRepository.findByCashierIdAndCreatedAtBetween(
                user.getId(),
                shiftReport.getShiftStart(), shiftReport.getShiftEnd()
        );

        double totalRefunds = refunds.stream()
                .mapToDouble(refund -> refund.getAmount()!=null?
                        refund.getAmount():0.0).sum();


        double totalSales = orders.stream()
                .mapToDouble(Order :: getTotalAmount).sum();

        int totalOrders = orders.size();

        double netSales = totalSales-totalRefunds;

        shiftReport.setTotalRefunds(totalRefunds);
        shiftReport.setTotalSales(totalSales);
        shiftReport.setTotalOrders(totalOrders);
        shiftReport.setNetSale(netSales);
        shiftReport.setRecentOrders(getRecentOrders(orders));
        shiftReport.setTopSellingProducts(getTopSellingProducts(orders));
        shiftReport.setPaymentSummaries(getPaymentSummaries(orders, totalSales));
        shiftReport.setRefunds(refunds);

        ShiftReport savedReport = shiftReportRepository.save(shiftReport);

        return ShiftReportMapper.toDTO(savedReport);
    }

    @Override
    public ShiftReportDTO getShiftByCashierAndDate(Long cashierId,
                                                   LocalDateTime date) throws Exception {
        User cashier = userRepository.findById(cashierId).orElseThrow(
                ()-> new Exception("cashier id not found" + cashierId));
        LocalDateTime start = date.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = date.withHour(23).withMinute(59).withSecond(59);

        ShiftReport report = shiftReportRepository.findByCashierAndShiftStartBetween(
                cashier, start, end
        ).orElseThrow(()-> new Exception("Shift report nof found for date"));
        return ShiftReportMapper.toDTO(report);
    }

    //-------------------------------- Helper Methods ------------------------------

    private List<PaymentSummary> getPaymentSummaries(List<Order> orders,
                                                     double totalSales) {
        Map<PaymentType, List<Order>> grouped = orders.stream()
                .collect(Collectors.groupingBy(order ->order.getPaymentType()!=null?
                        order.getPaymentType():PaymentType.CASH));

        List<PaymentSummary> summaries = new ArrayList<>();
        for(Map.Entry<PaymentType, List<Order>> entry : grouped.entrySet()){
            double amount = entry.getValue().stream()
                    .mapToDouble(Order::getTotalAmount).sum();

            int transactions = entry.getValue().size();
            double percent = (amount/totalSales)*100;

            PaymentSummary ps = new PaymentSummary();
            ps.setType(entry.getKey());
            ps.setTotalAmount(amount);
            ps.setTransactionCount(transactions);
            ps.setPercentage(percent);
            summaries.add(ps);
        }
        return summaries;
    }
    private List<Product> getTopSellingProducts(List<Order> orders) {
        Map<Product,Integer> productSalesMap = new HashMap<>();
        for(Order order:orders){
            for(OrderItem item: order.getItems()){
                Product product = item.getProduct();
                productSalesMap.put(product, productSalesMap.getOrDefault(product, 0)+item.getQuantity());

            }
        }
        return productSalesMap.entrySet().stream()
                .sorted((a,b)->b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<Order> getRecentOrders(List<Order> orders) {
        return orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

}
