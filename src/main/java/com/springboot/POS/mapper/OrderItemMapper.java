package com.springboot.POS.mapper;

import com.springboot.POS.modal.OrderItem;
import com.springboot.POS.payload.dto.OrderItemDTO;
import com.springboot.POS.util.JsonLists;

public class OrderItemMapper {

    public static OrderItemDTO toDTO(OrderItem item){

        if(item == null) return null;
        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .unitPrice(item.getUnitPrice())
                .modifiers(JsonLists.stringList(item.getModifiersJson()))
                .kitchenNote(item.getKitchenNote())
                .dosage(item.getDosage())
                .serials(JsonLists.stringList(item.getSerialsJson()))
                .product(ProductMapper.toDTO(item.getProduct()))
                .build();
    }
}
