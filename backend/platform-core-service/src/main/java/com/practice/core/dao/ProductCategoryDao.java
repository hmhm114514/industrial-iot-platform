package com.practice.core.dao;
import com.practice.core.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryDao extends JpaRepository<ProductCategory, Long> {  }
