package com.practice.core.dao;
import com.practice.core.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductDao extends JpaRepository<Product, Long> {  }
