package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class RuleAudit extends BaseEntity { public Long ruleId; public Long deviceId; public String result; public String detail; }
