package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class Role extends BaseEntity { public String permissions; }
