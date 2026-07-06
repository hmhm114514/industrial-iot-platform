package com.practice.core.entity;
    import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.practice.common.BaseEntity;

    @Entity
    public class TaskLog extends BaseEntity { public Long taskId; public String result; public String detail; public LocalDateTime executeTime = LocalDateTime.now(); }
