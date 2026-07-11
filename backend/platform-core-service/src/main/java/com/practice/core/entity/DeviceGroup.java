package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class DeviceGroup extends BaseEntity { public Long parentId; public String location; public String owner; public Integer deviceCount = 0; }
