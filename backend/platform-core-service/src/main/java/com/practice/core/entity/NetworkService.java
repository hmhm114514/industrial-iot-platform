package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class NetworkService extends BaseEntity { public String type; public String host; public Integer port; public Long upMessages = 0L; public Long downMessages = 0L; }
