package com.springboot.POS.modal;

import com.springboot.POS.domain.OrderStatus;
import com.springboot.POS.domain.PaymentType;
import jakarta.persistence.*;
import lombok.*;

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

    private Double totalAmount;

    private Double taxAmount;

    private Double discount;

    private String discountType;

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

    private PaymentType paymentType;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = OrderStatus.PENDING;
    }

}
