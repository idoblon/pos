package com.springboot.POS.mapper;

import com.springboot.POS.modal.Product;
import com.springboot.POS.modal.Store;
import com.springboot.POS.payload.dto.ProductDTO;

public class ProductMapper {
    public ProductDTO toDTO(Product product){
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .desciption(product.getDesciption())
                .mrp(product.getMrp())
                .sellingPrice(product.getSellingPrice())
                .brand(product.getBrand())
                .storeId(product.getStore()!=null?product.getStore().getId():null)
                .image(product.getImage())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
      //            .categoryId(product.ge)
    }

    public Product toEntity(ProductDTO productDTO, Store store) {
        return Product.builder()
                .name(productDTO.getName())
                .sku(productDTO.getSku())
                .desciption(productDTO.getDesciption())
                .mrp(productDTO.getMrp())
                .sellingPrice(productDTO.getSellingPrice())
                .brand(productDTO.getBrand())
                .build();
    }
}
