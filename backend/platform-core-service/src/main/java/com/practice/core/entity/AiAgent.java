package com.practice.core.entity;
    import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.practice.common.BaseEntity;

    @Entity
    public class AiAgent extends BaseEntity { public String scenario; @Column(length=1000) public String description; @Column(length=4000) public String systemPrompt; public String modelName; public Boolean enabled = true; }
