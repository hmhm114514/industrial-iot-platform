package com.practice.core.dao;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.time.LocalDateTime;
import java.util.List;

    public interface DeviceDao extends JpaRepository<Device, Long> { long countByStatus(String status); List<Device> findByCreatedAtAfter(LocalDateTime start); Device findByDeviceKey(String deviceKey); Device findByCode(String code); }
