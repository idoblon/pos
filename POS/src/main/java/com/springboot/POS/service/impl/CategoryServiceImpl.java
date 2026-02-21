package com.springboot.POS.service.impl;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.CategoryMapper;
import com.springboot.POS.modal.Category;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.CategoryDTO;
import com.springboot.POS.repository.CategoryRepository;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.service.CategoryService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final StoreRepository storeRepository;
    @Override
    public CategoryDTO createCategory(CategoryDTO dto) throws Exception {
        User user = userService.getCurrentUser();

        Store store = storeRepository.findById(dto.getStoreId()).orElseThrow(
                () -> new Exception("Store not found")
        );
        Category category = Category.builder()
                .store(store)
                .name(dto.getName())
                .build();

        checkAuthority(user, category.getStore());


        return CategoryMapper.toDTO(categoryRepository.save(category));
    }

    @Override
    public List<CategoryDTO> getCategoriesByStore(Long storeId) {
        List<Category> categories = categoryRepository.findByStoreId(storeId);
        return categories.stream()
                .map(
                        CategoryMapper::toDTO
                ).collect(Collectors.toList());
    }

    @Override
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) throws Exception {
        Category category = categoryRepository.findById(id).orElseThrow(
                () -> new Exception("Category does not exit")
        );
        User user = userService.getCurrentUser();

        category.setName(dto.getName());
        checkAuthority(user, category.getStore());

        return CategoryMapper.toDTO(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long id) throws Exception {
        Category category = categoryRepository.findById(id).orElseThrow(
                () -> new Exception("Category does not exit")
        );
        User user = userService.getCurrentUser();

        checkAuthority(user, category.getStore());

        categoryRepository.delete(category);
    }

    private void checkAuthority(User user, Store store) throws Exception {
        boolean isStoreAdmin = user.getRole().equals(UserRole.ROLE_STORE_ADMIN) &&
                store.getStoreAdmin() != null &&
                user.getId().equals(store.getStoreAdmin().getId());

        boolean isManager = user.getRole().equals(UserRole.ROLE_STORE_MANAGER);

        if (!isStoreAdmin && !isManager) {
            throw new Exception("You don't have permission to manage this category");
        }
    }
}
