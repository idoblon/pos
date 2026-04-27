package com.springboot.POS.service;

import com.springboot.POS.payload.dto.CategoryDTO;

import java.util.List;
import java.util.Map;

public interface CategoryService {
    CategoryDTO createCategory(CategoryDTO dto) throws Exception;

    List<CategoryDTO> getCategoriesByStore(Long storeId);

    CategoryDTO updateCategory(Long id, CategoryDTO dto) throws Exception;

    CategoryDTO patchCategory(Long id, Map<String, Object> updates) throws Exception;

    void deleteCategory(Long id) throws Exception;
}
