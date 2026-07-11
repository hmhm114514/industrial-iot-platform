package com.practice.core.dao;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
    import java.time.LocalDateTime;
import java.util.List;

    public interface AlarmDao extends JpaRepository<Alarm, Long>, JpaSpecificationExecutor<Alarm> { long countByStatus(String status); List<Alarm> findTop20ByOrderByCreatedAtDesc(); List<Alarm> findByCreatedAtAfter(LocalDateTime start); }
