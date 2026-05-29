package com.springboot.POS.domain;

public enum StockMovementType {
    INITIAL_STOCK,      // When inventory is first created
    RESTOCK,            // When stock is added via restock request
    MANUAL_ADJUSTMENT,  // Manual stock update by manager
    SALE,               // Stock deducted due to order
    REFUND,             // Stock added back due to refund
    DAMAGE,             // Stock removed due to damage
    TRANSFER_IN,        // Stock received from another branch
    TRANSFER_OUT        // Stock sent to another branch
}
