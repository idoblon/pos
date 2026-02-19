package com.springboot.POS.service;

import com.springboot.POS.modal.Product;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.ProductDTO;

import java.util.List;

public interface ProductService {

    ProductDTO createProduct(ProductDTO productDTO, User user);
    ProductDTO updateProduct(Long id, ProductDTO productDTO, User user);
    void deleteProduct(Long id, User user);

    List<ProductDTO> getProductsByStoreId(Long storeId);
    List<ProductDTO> searchByKeyword(Long storeId, String keyword);
}
