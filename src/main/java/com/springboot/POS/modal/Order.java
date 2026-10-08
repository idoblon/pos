package com.springboot.POS.modal;

import com.springboot.POS.domain.OrderStatus;
import com.springboot.POS.domain.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    private BigDecimal discount;

    private String discountType;

    @Column(length = 1000)
    private String note;

    @Column(name = "idempotency_key", unique = true, length = 255)
    private String idempotencyKey;

    @Column(name = "created_at")
    @org.hibernate.annotations.CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    private Branch branch;

    @ManyToOne
    private User cashier;

    @ManyToOne
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    // Stores the gateway reference returned from the popup (eSewa ref, Khalti token, card last-4)
    private String paymentReference;

    // CASH only: raw amount handed over by customer
    private BigDecimal amountReceived;

    /** Restaurant: DINE_IN, TAKEAWAY or DELIVERY. */
    @Column(length = 16)
    private String orderType;

    /** Restaurant table label (e.g. "T4"). */
    @Column(length = 32)
    private String tableNumber;

    /** Free-text note for the kitchen (KOT). */
    @Column(length = 500)
    private String kitchenNote;

    /** Pharmacy: pharmacist sign-off on attached prescription. */
    @Builder.Default
    private Boolean prescriptionVerified = false;

    /** Electronics: EMI tenure in months (3/6/12), null = full payment. */
    private Integer emiMonths;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = OrderStatus.PENDING;
        if (prescriptionVerified == null) prescriptionVerified = false;
    }

}
