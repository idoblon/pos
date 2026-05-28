package com.springboot.POS.mapper;

import com.springboot.POS.modal.Category;
import com.springboot.POS.modal.Product;
import com.springboot.POS.modal.Store;
import com.springboot.POS.payload.dto.ProductDTO;

public class ProductMapper {
    public static ProductDTO toDTO(Product product){
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .mrp(product.getMrp())
                .sellingPrice(product.getSellingPrice())
                .brand(product.getBrand())
                .category(CategoryMapper.toDTO(product.getCategory()))
                .storeId(product.getStore()!=null?product.getStore().getId():null)
                .image(product.getImage())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();

    }

    public static Product toEntity(ProductDTO productDTO, Store store,
                                   Category category) {
        return Product.builder()
                .name(productDTO.getName())
                .store(store)
                .sku(productDTO.getSku())
                .category(category)
                .mrp(productDTO.getMrp())
                .sellingPrice(productDTO.getSellingPrice())
                .brand(productDTO.getBrand())
                .image(productDTO.getImage())
                .build();
    }
}
