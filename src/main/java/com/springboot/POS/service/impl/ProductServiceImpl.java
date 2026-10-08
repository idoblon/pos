package com.springboot.POS.service.impl;

import com.springboot.POS.mapper.ProductMapper;
import com.springboot.POS.modal.*;
import com.springboot.POS.payload.dto.ProductDTO;
import com.springboot.POS.repository.*;
import com.springboot.POS.service.ProductService;
import com.springboot.POS.service.SubscriptionLimitService;
import com.springboot.POS.util.JsonLists;
import com.springboot.POS.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

        private final ProductRepository productRepository;
        private final StoreRepository storeRepository;
        private final CategoryRepository categoryRepository;
        private final BranchRepository branchRepository;
        private final InventoryRepository inventoryRepository;
        private final SubscriptionLimitService limitService;

        @Override
        public ProductDTO createProduct(ProductDTO productDTO, User user) throws Exception {

                Store store = storeRepository.findById(
                                productDTO.getStoreId()).orElseThrow(
                                                () -> new Exception("Store not found"));

                Category category = categoryRepository.findById(productDTO.getCategoryId()).orElseThrow(
                                                () -> new Exception("Category not Found"));

                // Storage quota: product images are the metered media.
                if (productDTO.getImage() != null && !productDTO.getImage().isEmpty()) {
                        limitService.requireStorageFor(store, productDTO.getImage().length());
                }

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
                                log.debug("Created inventory for product {} in branch {}",
                                        savedProduct.getName(), branch.getName());
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
                        log.debug("Updating image, length: {}", productDTO.getImage().length());
                        long oldBytes = product.getImage() != null ? product.getImage().length() : 0L;
                        Store imageStore = product.getStore();
                        if (imageStore != null) {
                                limitService.requireStorageForReplacement(
                                                imageStore, oldBytes, productDTO.getImage().length());
                        }
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
                // Vertical attributes: null in a PATCH means "leave unchanged".
                if (productDTO.getExpiryDate() != null) {
                        product.setExpiryDate(productDTO.getExpiryDate());
                }
                if (productDTO.getBatchNumber() != null) {
                        product.setBatchNumber(productDTO.getBatchNumber());
                }
                if (productDTO.getPrescriptionRequired() != null) {
                        product.setPrescriptionRequired(productDTO.getPrescriptionRequired());
                }
                if (productDTO.getControlledSubstance() != null) {
                        product.setControlledSubstance(productDTO.getControlledSubstance());
                }
                if (productDTO.getDosage() != null) {
                        product.setDosage(productDTO.getDosage());
                }
                if (productDTO.getUnit() != null) {
                        product.setUnit(productDTO.getUnit());
                }
                if (productDTO.getWeight() != null) {
                        product.setWeight(productDTO.getWeight());
                }
                if (productDTO.getWeightStep() != null) {
                        product.setWeightStep(productDTO.getWeightStep());
                }
                if (productDTO.getMoq() != null) {
                        product.setMoq(productDTO.getMoq());
                }
                if (productDTO.getRequiresSerial() != null) {
                        product.setRequiresSerial(productDTO.getRequiresSerial());
                }
                if (productDTO.getWarrantyMonths() != null) {
                        product.setWarrantyMonths(productDTO.getWarrantyMonths());
                }
                if (productDTO.getSizeVariant() != null) {
                        product.setSizeVariant(productDTO.getSizeVariant());
                }
                if (productDTO.getColorVariant() != null) {
                        product.setColorVariant(productDTO.getColorVariant());
                }
                if (productDTO.getVariants() != null) {
                        product.setVariantsJson(JsonLists.toJson(productDTO.getVariants()));
                }
                if (productDTO.getBulkMinQty() != null) {
                        product.setBulkMinQty(productDTO.getBulkMinQty());
                }
                if (productDTO.getBulkPrice() != null) {
                        product.setBulkPrice(productDTO.getBulkPrice());
                }
                if (productDTO.getBulkTiers() != null) {
                        product.setBulkTiersJson(JsonLists.toJson(productDTO.getBulkTiers()));
                }
                if (productDTO.getPreparationTime() != null) {
                        product.setPreparationTime(productDTO.getPreparationTime());
                }
                if (productDTO.getKitchenStation() != null) {
                        product.setKitchenStation(productDTO.getKitchenStation());
                }
                if (productDTO.getModifiers() != null) {
                        product.setModifiersJson(JsonLists.toJson(productDTO.getModifiers()));
                }
                if (productDTO.getIsVeg() != null) {
                        product.setIsVeg(productDTO.getIsVeg());
                }
                if (productDTO.getCareInstructions() != null) {
                        product.setCareInstructions(productDTO.getCareInstructions());
                }
                if (productDTO.getGuaranteeDays() != null) {
                        product.setGuaranteeDays(productDTO.getGuaranteeDays());
                }
                product.setUpdatedAt(LocalDateTime.now());

                if (productDTO.getCategoryId() != null) {
                        Category category = categoryRepository.findById(productDTO.getCategoryId()).orElseThrow(
                                        () -> new Exception("Category not found."));
                        product.setCategory(category);
                }

                Product savedProduct = productRepository.save(product);
                log.debug("Saved product image length: {}",
                        savedProduct.getImage() != null ? savedProduct.getImage().length() : -1);
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
