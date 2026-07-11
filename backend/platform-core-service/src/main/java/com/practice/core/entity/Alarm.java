package com.practice.core.entity;
    import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.practice.common.BaseEntity;

    @Entity
    public class Alarm extends BaseEntity { public Long deviceId; public String deviceName; public Long ruleId; public String level; public String type; public String ruleName; public String content; public String handler; public LocalDateTime handledAt; }
