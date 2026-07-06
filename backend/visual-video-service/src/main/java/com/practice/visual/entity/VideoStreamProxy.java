package com.practice.visual.entity;
import jakarta.persistence.Entity;
import com.practice.common.BaseEntity;
@Entity public class VideoStreamProxy extends BaseEntity { public Long videoDeviceId; public String playUrl; public String protocol; }
