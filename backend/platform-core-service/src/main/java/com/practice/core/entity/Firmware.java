package com.practice.core.entity;
    import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;

    @Entity
    public class Firmware extends BaseEntity { public String version; public String targetProduct; public String upgradeStatus = "READY"; public String fileUrl; }
