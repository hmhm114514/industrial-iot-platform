package com.practice.visual.entity;
import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;
@Entity public class VideoAlarmTask extends BaseEntity { public Long videoDeviceId; public String algorithm; public Boolean enabled = true; }
