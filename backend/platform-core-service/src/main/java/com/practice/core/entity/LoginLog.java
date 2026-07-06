package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class LoginLog extends BaseEntity { public String username; public String ip; public Boolean success; }
