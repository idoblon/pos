package com.springboot.POS.repository;

import com.springboot.POS.modal.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreId(Long storeId);

    Page<Product> findByStoreId(Long storeId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND (p.deleted = false OR p.deleted IS NULL)")
    List<Product> findByStoreIdAndDeletedFalse(@Param("storeId") Long storeId);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND (p.deleted = false OR p.deleted IS NULL)")
    Page<Product> findByStoreIdAndDeletedFalse(@Param("storeId") Long storeId, Pageable pageable);

    @Query(
            "select p from Product p " +
             "where p.store.id = :storeId and p.deleted = false and (" +
            "lower(p.name) like lower(concat('%', :query, '%'))"+
            "or lower(p.brand) like lower(concat('%', :query, '%'))"+
                    "or lower(p.sku) like lower(concat('%', :query, '%'))"+
                    ") order by " +
            "case when lower(p.sku) = lower(:query) then 0 " +
            "when lower(p.name) = lower(:query) then 1 " +
            "when lower(p.name) like lower(concat(:query, '%')) then 2 " +
            "else 3 end"
    )
    List<Product> searchByKeyword(@Param("storeId") Long storeId,
                                  @Param("query") String keyword);
}
