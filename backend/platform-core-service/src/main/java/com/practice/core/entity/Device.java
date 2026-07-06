package com.practice.core.entity;
    import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.practice.common.BaseEntity;

    @Entity
    public class Device extends BaseEntity { public Long productId; public Long groupId; public String deviceKey; public String location; public Double latitude; public Double longitude; public LocalDateTime lastOnlineAt; }
