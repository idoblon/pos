package com.springboot.POS.mapper;

import com.springboot.POS.modal.Category;
import com.springboot.POS.modal.Product;
import com.springboot.POS.modal.Store;
import com.springboot.POS.payload.dto.BulkTierDTO;
import com.springboot.POS.payload.dto.ProductDTO;
import com.springboot.POS.util.JsonLists;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductMapper {
    public static ProductDTO toDTO(Product product){
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .mrp(product.getMrp())
                .sellingPrice(product.getSellingPrice())
                .brand(product.getBrand())
                .category(CategoryMapper.toDTO(product.getCategory()))
                .storeId(product.getStore()!=null?product.getStore().getId():null)
                .image(product.getImage())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .expiryDate(product.getExpiryDate())
                .batchNumber(product.getBatchNumber())
                .prescriptionRequired(product.getPrescriptionRequired())
                .controlledSubstance(product.getControlledSubstance())
                .dosage(product.getDosage())
                .unit(product.getUnit())
                .weight(product.getWeight())
                .weightStep(product.getWeightStep())
                .moq(product.getMoq())
                .requiresSerial(product.getRequiresSerial())
                .warrantyMonths(product.getWarrantyMonths())
                .sizeVariant(product.getSizeVariant())
                .colorVariant(product.getColorVariant())
                .variants(JsonLists.mapList(product.getVariantsJson()))
                .bulkMinQty(product.getBulkMinQty())
                .bulkPrice(product.getBulkPrice())
                .bulkTiers(toBulkTierDTOs(product.getBulkTiersJson()))
                .preparationTime(product.getPreparationTime())
                .kitchenStation(product.getKitchenStation())
                .modifiers(JsonLists.stringList(product.getModifiersJson()))
                .isVeg(product.getIsVeg())
                .careInstructions(product.getCareInstructions())
                .guaranteeDays(product.getGuaranteeDays())
                .build();

    }

    public static Product toEntity(ProductDTO productDTO, Store store,
                                   Category category) {
        return Product.builder()
                .name(productDTO.getName())
                .store(store)
                .sku(productDTO.getSku())
                .category(category)
                .description(productDTO.getDescription())
                .mrp(productDTO.getMrp())
                .sellingPrice(productDTO.getSellingPrice())
                .brand(productDTO.getBrand())
                .image(productDTO.getImage())
                .expiryDate(productDTO.getExpiryDate())
                .batchNumber(productDTO.getBatchNumber())
                .prescriptionRequired(productDTO.getPrescriptionRequired())
                .controlledSubstance(productDTO.getControlledSubstance())
                .dosage(productDTO.getDosage())
                .unit(productDTO.getUnit())
                .weight(productDTO.getWeight())
                .weightStep(productDTO.getWeightStep())
                .moq(productDTO.getMoq())
                .requiresSerial(productDTO.getRequiresSerial())
                .warrantyMonths(productDTO.getWarrantyMonths())
                .sizeVariant(productDTO.getSizeVariant())
                .colorVariant(productDTO.getColorVariant())
                .variantsJson(JsonLists.toJson(productDTO.getVariants()))
                .bulkMinQty(productDTO.getBulkMinQty())
                .bulkPrice(productDTO.getBulkPrice())
                .bulkTiersJson(JsonLists.toJson(productDTO.getBulkTiers()))
                .preparationTime(productDTO.getPreparationTime())
                .kitchenStation(productDTO.getKitchenStation())
                .modifiersJson(JsonLists.toJson(productDTO.getModifiers()))
                .isVeg(productDTO.getIsVeg())
                .careInstructions(productDTO.getCareInstructions())
                .guaranteeDays(productDTO.getGuaranteeDays())
                .build();
    }

    private static List<BulkTierDTO> toBulkTierDTOs(String json) {
        return JsonLists.mapList(json).stream()
                .map(ProductMapper::toBulkTierDTO)
                .collect(Collectors.toList());
    }

    private static BulkTierDTO toBulkTierDTO(Map<String, Object> map) {
        return BulkTierDTO.builder()
                .minQty(toInteger(map.get("minQty")))
                .price(toDecimal(map.get("price")))
                .build();
    }

    private static Integer toInteger(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(String.valueOf(value)).intValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
