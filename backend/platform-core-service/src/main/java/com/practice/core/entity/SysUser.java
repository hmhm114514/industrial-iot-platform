package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class SysUser extends BaseEntity { public String username; public String password; public String realName; public String roleName; }
