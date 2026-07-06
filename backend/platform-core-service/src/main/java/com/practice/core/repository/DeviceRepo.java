package com.practice.core.repository;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.time.LocalDateTime;
import java.util.List;

    public interface DeviceRepo extends JpaRepository<Device, Long> { long countByStatus(String status); List<Device> findByCreatedAtAfter(LocalDateTime start); }
