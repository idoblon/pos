package com.springboot.POS.service.impl;

import com.springboot.POS.mapper.ProductMapper;
import com.springboot.POS.modal.*;
import com.springboot.POS.payload.dto.ProductDTO;
import com.springboot.POS.repository.*;
import com.springboot.POS.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        private final BranchRepository branchRepository;
        private final InventoryRepository inventoryRepository;

        @Override
        public ProductDTO createProduct(ProductDTO productDTO, User user) throws Exception {

                Store store = storeRepository.findById(
                                productDTO.getStoreId()).orElseThrow(
                                                () -> new Exception("Store not found"));

                Category category = categoryRepository.findById(productDTO.getCategoryId()).orElseThrow(
                                () -> new Exception("Category not Found"));

                Product product = ProductMapper.toEntity(productDTO, store, category);
                Product savedProduct = productRepository.save(product);

                // Auto-create inventory for all branches under this store
                List<Branch> branches = branchRepository.findByStoreIdAndDeletedFalse(store.getId());
                for (Branch branch : branches) {
                        // Check if inventory already exists
                        List<Inventory> existing = inventoryRepository.findByProductIdAndBranchId(
                                savedProduct.getId(), branch.getId());
                        
                        if (existing.isEmpty()) {
                                Inventory inventory = Inventory.builder()
                                        .product(savedProduct)
                                        .branch(branch)
                                        .quantity(0)
                                        .build();
                                inventoryRepository.save(inventory);
                                System.out.println("✅ Created inventory for product " + savedProduct.getName() + 
                                        " in branch " + branch.getName());
                        }
                }

                return ProductMapper.toDTO(savedProduct);
        }

        @Override
        public ProductDTO updateProduct(Long id, ProductDTO productDTO, User user) throws Exception {
                Product product = productRepository.findById(id).orElseThrow(
                                () -> new Exception("product not found."));

                if (productDTO.getName() != null) {
                        product.setName(productDTO.getName());
                }
                if (productDTO.getDescription() != null) {
                        product.setDescription(productDTO.getDescription());
                }
                if (productDTO.getSku() != null) {
                        product.setSku(productDTO.getSku());
                }
                if (productDTO.getImage() != null && !productDTO.getImage().isEmpty()) {
                        System.out.println("📸 Updating image, length: " + productDTO.getImage().length());
                        product.setImage(productDTO.getImage());
                }
                if (productDTO.getMrp() != null) {
                        product.setMrp(productDTO.getMrp());
                }
                if (productDTO.getSellingPrice() != null) {
                        product.setSellingPrice(productDTO.getSellingPrice());
                }
                if (productDTO.getBrand() != null) {
                        product.setBrand(productDTO.getBrand());
                }
                product.setUpdatedAt(LocalDateTime.now());

                if (productDTO.getCategoryId() != null) {
                        Category category = categoryRepository.findById(productDTO.getCategoryId()).orElseThrow(
                                        () -> new Exception("Category not found."));
                        product.setCategory(category);
                }

                Product savedProduct = productRepository.save(product);
                System.out.println("💾 Saved product image length: " + (savedProduct.getImage() != null ? savedProduct.getImage().length() : "null"));
                return ProductMapper.toDTO(savedProduct);
        }

        @Override
        public void deleteProduct(Long id, User user) throws Exception {

                Product product = productRepository.findById(id).orElseThrow(
                                () -> new Exception("product not found"));

                product.setDeleted(true);
                productRepository.save(product);
        }

        @Override
        public List<ProductDTO> getProductsByStoreId(Long storeId) {
                return productRepository.findByStoreIdAndDeletedFalse(storeId).stream()
                                .map(ProductMapper::toDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public Page<ProductDTO> getProductsByStoreId(Long storeId, Pageable pageable) {
                return productRepository.findByStoreIdAndDeletedFalse(storeId, pageable)
                                .map(ProductMapper::toDTO);
        }

        @Override
        public List<ProductDTO> searchByKeyword(Long storeId, String keyword) {
                List<Product> product = productRepository.searchByKeyword(storeId, keyword);
                return product.stream()
                                .map(ProductMapper::toDTO)
                                .collect(Collectors.toList());
        }
}
