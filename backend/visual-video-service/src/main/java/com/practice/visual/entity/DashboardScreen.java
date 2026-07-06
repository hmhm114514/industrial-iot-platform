package com.practice.visual.entity;
import jakarta.persistence.*;
import com.practice.common.BaseEntity;
@Entity public class DashboardScreen extends BaseEntity { public String groupName; public Boolean published = false; @Column(length=4000) public String configJson; }
