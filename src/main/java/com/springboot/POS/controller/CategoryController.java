package com.springboot.POS.controller;

import com.springboot.POS.modal.Category;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.CategoryDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.repository.CategoryRepository;
import com.springboot.POS.service.CategoryService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

        private final CategoryService categoryService;
        private final UserService userService;
        private final OwnershipGuard ownershipGuard;
        private final CategoryRepository categoryRepository;

        private void guardCategoryStore(User user, Long categoryId) throws Exception {
                Category category = categoryRepository.findById(categoryId)
                                .orElseThrow(() -> new com.springboot.POS.exceptions.UserException(
                                                "Category not found"));
                Long storeId = category.getStore() != null ? category.getStore().getId() : null;
                ownershipGuard.requireStoreAccess(user, storeId);
        }

        @PostMapping
        public ResponseEntity<CategoryDTO> createCategory(
                        @RequestBody CategoryDTO categoryDTO,
                        @RequestHeader("Authorization") String jwt) throws Exception {
                User user = userService.getUserFromJwtToken(jwt);
                ownershipGuard.requireStoreAccess(user, categoryDTO.getStoreId());
                return ResponseEntity.ok(
                                categoryService.createCategory(categoryDTO));
        }

        @GetMapping("/store/{storeId}")
        public ResponseEntity<List<CategoryDTO>> getCategoryByStoreId(
                        @PathVariable Long storeId,
                        @RequestHeader("Authorization") String jwt) throws Exception {
                User user = userService.getUserFromJwtToken(jwt);
                ownershipGuard.requireStoreAccess(user, storeId);
                return ResponseEntity.ok(
                                categoryService.getCategoriesByStore(storeId));
        }

        @PutMapping("/{id}")
        public ResponseEntity<CategoryDTO> updateCategory(
                        @RequestBody CategoryDTO categoryDTO,
                        @PathVariable Long id,
                        @RequestHeader("Authorization") String jwt) throws Exception {
                User user = userService.getUserFromJwtToken(jwt);
                guardCategoryStore(user, id);
                return ResponseEntity.ok(
                                categoryService.updateCategory(id, categoryDTO));
        }

        @PatchMapping("/{id}")
        public ResponseEntity<CategoryDTO> patchCategory(
                        @RequestBody Map<String, Object> updates,
                        @PathVariable Long id,
                        @RequestHeader("Authorization") String jwt) throws Exception {
                User user = userService.getUserFromJwtToken(jwt);
                guardCategoryStore(user, id);
                return ResponseEntity.ok(
                                categoryService.patchCategory(id, updates));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse> deleteCategory(
                        @PathVariable Long id,
                        @RequestHeader("Authorization") String jwt) throws Exception {
                User user = userService.getUserFromJwtToken(jwt);
                guardCategoryStore(user, id);
                categoryService.deleteCategory(id);
                ApiResponse apiResponse = new ApiResponse();
                apiResponse.setMessage("Category Successfully deleted");
                return ResponseEntity.ok(
                                apiResponse);
        }
}
