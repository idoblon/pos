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

    /**
     * Exact billed amount in the product's selling unit (supports weighted
     * goods like 0.5 kg). Integer-unit stock deduction uses the ceiling.
     */
    @Column(precision = 19, scale = 3)
    private BigDecimal quantity;

    private BigDecimal price;

    private BigDecimal unitPrice;

    /** Chosen modifiers ["Extra cheese"] as JSON. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String modifiersJson;

    /** Per-item kitchen instruction. */
    @Column(length = 255)
    private String kitchenNote;

    /** Per-item pharmacy dosage label. */
    @Column(length = 255)
    private String dosage;

    /** Captured serials/IMEIs (one per unit) as JSON. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String serialsJson;

    @ManyToOne
    private Product product;

    @ManyToOne
    private Order order;

}
