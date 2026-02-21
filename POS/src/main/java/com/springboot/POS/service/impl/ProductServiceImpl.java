package com.springboot.POS.service.impl;

import com.springboot.POS.mapper.ProductMapper;
import com.springboot.POS.modal.Category;
import com.springboot.POS.modal.Product;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.ProductDTO;
import com.springboot.POS.repository.CategoryRepository;
import com.springboot.POS.repository.ProductRepository;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public ProductDTO createProduct(ProductDTO productDTO, User user) throws Exception {

        Store store = storeRepository.findById(
                productDTO.getStoreId()
        ).orElseThrow(
                ()-> new Exception("Store not found")
        );

        Category category = categoryRepository.findById(productDTO.getCategoryId()).orElseThrow(
                () -> new Exception("Category not Found")
        );

        Product product = ProductMapper.toEntity(productDTO, store, category);
        Product savedProduct = productRepository.save(product);
        return ProductMapper.toDTO(savedProduct);
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO, User user) throws Exception {
        Product product = productRepository.findById(id).orElseThrow(
                ()-> new Exception("product not found.")
        );

        product.setName(productDTO.getName());
        product.setDesciption(productDTO.getDesciption());
        product.setSku(productDTO.getSku());
        product.setImage(product.getImage());
        product.setMrp(product.getMrp());
        product.setSellingPrice(product.getSellingPrice());
        product.setBrand(product.getBrand());
        product.setUpdatedAt(LocalDateTime.now());

        if(productDTO.getCategoryId()!=null){
            Category category = categoryRepository.findById(productDTO.getCategoryId()).orElseThrow(
                    () -> new Exception("Category not found.")
            );
            product.setCategory(category);
        }

        Product savedProduct = productRepository.save(product);
        return ProductMapper.toDTO(savedProduct);
    }

    @Override
    public void deleteProduct(Long id, User user) throws Exception {

        Product product = productRepository.findById(id).orElseThrow(
                ()-> new Exception("product not found")
        );

        productRepository.delete(product);
    }

    @Override
    public List<ProductDTO> getProductsByStoreId(Long storeId) {
        List<Product> product = productRepository.findByStoreId(storeId);
        return product.stream()
                .map(ProductMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> searchByKeyword(Long storeId, String keyword) {
        List<Product> product = productRepository.searchByKeyword(storeId, keyword);
        return product.stream()
                .map(ProductMapper::toDTO)
                .collect(Collectors.toList());
    }
}
