package com.springboot.POS.modal;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal unitPrice;

    @ManyToOne
    private Product product;

    @ManyToOne
    private Order order;

}
