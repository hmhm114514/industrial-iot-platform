package com.practice.core.repository;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.time.LocalDateTime;
import java.util.List;

    public interface AlarmRepo extends JpaRepository<Alarm, Long> { long countByStatus(String status); List<Alarm> findTop20ByOrderByCreatedAtDesc(); List<Alarm> findByCreatedAtAfter(LocalDateTime start); }
