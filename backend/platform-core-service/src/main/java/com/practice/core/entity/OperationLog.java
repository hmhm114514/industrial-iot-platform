package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class OperationLog extends BaseEntity { public String moduleName; public String action; public String operator; public String detail; }
