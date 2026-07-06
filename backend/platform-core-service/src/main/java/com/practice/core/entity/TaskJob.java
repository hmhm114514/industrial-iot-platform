package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class TaskJob extends BaseEntity { public String cron; public Boolean running = false; public String lastResult; }
