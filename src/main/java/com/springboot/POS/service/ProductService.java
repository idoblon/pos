package com.springboot.POS.service;

import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductDTO createProduct(ProductDTO productDTO, User user) throws Exception;
    ProductDTO updateProduct(Long id, ProductDTO productDTO, User user) throws Exception;
    void deleteProduct(Long id, User user) throws Exception;

    List<ProductDTO> getProductsByStoreId(Long storeId);
    Page<ProductDTO> getProductsByStoreId(Long storeId, Pageable pageable);
    List<ProductDTO> searchByKeyword(Long storeId, String keyword);
}
