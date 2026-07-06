package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class Rule extends BaseEntity { public String metric = "temperature"; public String operator = ">"; public Double threshold = 80.0; public Boolean enabled = true; public String alarmLevel = "HIGH"; }
