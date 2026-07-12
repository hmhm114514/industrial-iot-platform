package com.practice.core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TelemetryData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long deviceId; public String deviceName;
    public Double temperature; public Double humidity; public Double pressure;
    public String dataStatus = "NORMAL";
    @Column(length=1000) public String dataMessage;
    @Column(length=2000) public String payload;
    public LocalDateTime reportTime = LocalDateTime.now();
}
