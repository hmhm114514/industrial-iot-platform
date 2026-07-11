package com.practice.core.entity;
    import jakarta.persistence.*;
import com.practice.common.BaseEntity;

    @Entity
    public class Product extends BaseEntity { public Long categoryId; public Long ruleId; public String ruleIds; public String protocol; public String manufacturer; @Transient public Integer deviceCount = 0; }
