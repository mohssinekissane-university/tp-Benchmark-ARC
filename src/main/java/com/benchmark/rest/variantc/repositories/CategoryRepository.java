package com.benchmark.rest.variantc.repositories;

import com.benchmark.rest.variantc.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
