package com.springboot.POS.modal;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"sku", "store_id"}))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String sku;

    private String description;

    @Column(precision = 19, scale = 2)
    private BigDecimal mrp;

    @Column(precision = 19, scale = 2)
    private BigDecimal sellingPrice;

    private String brand;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String image;

    private Boolean deleted = false;

    // ── Vertical store-type attributes (all optional) ──

    /** Perishables (pharmacy/grocery): sale blocked on/after this date. */
    private LocalDate expiryDate;

    @Column(length = 64)
    private String batchNumber;

    @Builder.Default
    private Boolean prescriptionRequired = false;

    @Builder.Default
    private Boolean controlledSubstance = false;

    /** Printed dosage / usage label (pharmacy). */
    @Column(length = 255)
    private String dosage;

    /** Selling unit: pcs, kg, g, L, box, ... */
    @Column(length = 16)
    private String unit;

    /** Nominal weight value in the selling unit. */
    @Column(precision = 19, scale = 3)
    private BigDecimal weight;

    /** Scale/cashier step for weighted goods (e.g. 0.5 kg). */
    @Column(precision = 19, scale = 3)
    private BigDecimal weightStep;

    /** Minimum order quantity (wholesale packs). */
    private Integer moq;

    @Builder.Default
    private Boolean requiresSerial = false;

    /** Warranty length in months (electronics). */
    private Integer warrantyMonths;

    @Column(length = 64)
    private String sizeVariant;

    @Column(length = 64)
    private String colorVariant;

    /** Variant matrix [{sku,size,color,price,stock}] as JSON. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String variantsJson;

    /** Legacy single bulk tier (kept alongside bulkTiersJson). */
    private Integer bulkMinQty;

    @Column(precision = 19, scale = 2)
    private BigDecimal bulkPrice;

    /** Multi-tier wholesale pricing [{minQty,price}] as JSON. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String bulkTiersJson;

    /** Minutes (restaurant menu items). */
    private Integer preparationTime;

    @Column(length = 64)
    private String kitchenStation;

    /** Modifier options ["Extra cheese","No onion"] as JSON. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String modifiersJson;

    @Builder.Default
    private Boolean isVeg = false;

    @Column(length = 512)
    private String careInstructions;

    /** Plant survival guarantee in days. */
    private Integer guaranteeDays;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "created_at")
    @org.hibernate.annotations.CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @org.hibernate.annotations.UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.deleted == null) {
            this.deleted = false;
        }
        if (this.prescriptionRequired == null) {
            this.prescriptionRequired = false;
        }
        if (this.controlledSubstance == null) {
            this.controlledSubstance = false;
        }
        if (this.requiresSerial == null) {
            this.requiresSerial = false;
        }
        if (this.isVeg == null) {
            this.isVeg = false;
        }
    }

}
