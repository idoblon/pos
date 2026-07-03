package com.springboot.POS.modal;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShiftReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;

    private Double totalSales;
    private Double totalRefunds;
    private Double netSale;
    private int totalOrders;

    @ManyToOne
    private User cashier;

    @ManyToOne
    private Branch branch;

    @Transient
    private List<PaymentSummary> paymentSummaries;

    @ManyToMany
    @JoinTable(name = "shift_report_top_selling_products",
            joinColumns = @JoinColumn(name = "shift_report_id"),
            inverseJoinColumns = @JoinColumn(name = "top_selling_products_id"))
    private List<Product> topSellingProducts;

    @ManyToMany
    @JoinTable(name = "shift_report_recent_orders",
            joinColumns = @JoinColumn(name = "shift_report_id"),
            inverseJoinColumns = @JoinColumn(name = "recent_orders_id"))
    private List<Order> recentOrders;

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL)
    private List<Refund> refunds;

    // Cash reconciliation
    private Double openingFloat;      // cash in drawer at shift start
    private Double declaredCash;      // cash cashier counts at shift end
    private Double expectedCash;      // calculated: openingFloat + cash sales - cash refunds
    private Double cashDiscrepancy;   // declaredCash - expectedCash
    private String reconciliationStatus; // MATCHED, SURPLUS, SHORTAGE

}
