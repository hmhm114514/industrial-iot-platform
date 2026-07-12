package com.practice.core.entity;

import com.practice.common.BaseEntity;
import jakarta.persistence.Entity;

@Entity
public class DeviceAttribute extends BaseEntity {
    public String valueType;
    public Boolean rangeEnabled;
    public Boolean minEnabled;
    public Boolean maxEnabled;
    public Double minValue;
    public Double maxValue;
}
