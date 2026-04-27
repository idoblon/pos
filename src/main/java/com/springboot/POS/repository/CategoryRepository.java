package com.springboot.POS.repository;

import com.springboot.POS.modal.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByStoreId(Long StoreId);
}
