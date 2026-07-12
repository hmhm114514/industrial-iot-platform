package com.practice.core.dao;

import com.practice.core.entity.DeviceAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceAttributeDao extends JpaRepository<DeviceAttribute, Long> {
    DeviceAttribute findByName(String name);
}
