package com.benchmark.rest.variantc.repositories;

import com.benchmark.rest.variantc.entities.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByCategoryId(Long categoryId, Pageable p);

    @Query("select i from Item i join fetch i.category where i.category.id = :cid")
    Page<Item> findByCategoryIdWithCategory(@Param("cid") Long categoryId, Pageable p);
}
