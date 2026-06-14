package com.springboot.POS.modal;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Branch branch;  // NULL = Warehouse inventory, NOT NULL = Branch inventory

    @ManyToOne
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // Unit price for inventory value calculation
    private Double unitPrice;

    // Store reference for warehouse inventory (when branch is null)
    @ManyToOne
    private Store store;

    private LocalDateTime lastUpdate;

    @PrePersist
    @PreUpdate
    protected  void onUpdate(){
        lastUpdate = LocalDateTime.now();
    }
}
