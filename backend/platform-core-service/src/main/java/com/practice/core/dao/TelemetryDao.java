package com.practice.core.dao;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.time.LocalDateTime;
import java.util.List;

    public interface TelemetryDao extends JpaRepository<TelemetryData, Long> { List<TelemetryData> findTop50ByOrderByReportTimeDesc(); List<TelemetryData> findAllByOrderByReportTimeDesc(); long countByReportTimeAfter(LocalDateTime time); List<TelemetryData> findByReportTimeAfter(LocalDateTime start); }
