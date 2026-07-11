package com.practice.core.entity;
    import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.practice.common.BaseEntity;

    @Entity
    public class Alarm extends BaseEntity { public Long deviceId; public String deviceName; public Long ruleId; public String level; public String type; public String ruleName; public String content; public String handler; public LocalDateTime acknowledgedAt; public LocalDateTime processingAt; public LocalDateTime recoveredAt; public LocalDateTime handledAt; public Long durationSeconds; @Column(length=2000) public String handlingRecord; }
