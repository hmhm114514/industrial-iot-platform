package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class Product extends BaseEntity { public Long categoryId; public String protocol; public String manufacturer; }
