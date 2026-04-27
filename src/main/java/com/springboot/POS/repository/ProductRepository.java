package com.springboot.POS.repository;

import com.springboot.POS.modal.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreId(Long storeId);

    @Query(
            "select p from Product p " +
             "where p.store.id = :storeId and (" +
            "lower(p.name) like lower(concat('%', :query, '%'))"+
            "or lower(p.brand) like lower(concat('%', :query, '%'))"+
                    "or lower(p.sku) like lower(concat('%', :query, '%'))"+
                    ")"
    )
    List<Product> searchByKeyword(@Param("storeId")Long storeId,
                                  @Param("query")String keyword);
}
