package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class Rule extends BaseEntity { public String metric = "temperature"; public String operator = ">"; public String threshold = "80"; public String logicOperator; public String secondMetric; public String secondOperator; public String secondThreshold; public Boolean enabled = true; public String alarmLevel = "HIGH"; public String action; }
