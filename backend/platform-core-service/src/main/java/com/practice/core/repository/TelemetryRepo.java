package com.practice.core.repository;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.time.LocalDateTime;
import java.util.List;

    public interface TelemetryRepo extends JpaRepository<TelemetryData, Long> { List<TelemetryData> findTop50ByOrderByReportTimeDesc(); long countByReportTimeAfter(LocalDateTime time); List<TelemetryData> findByReportTimeAfter(LocalDateTime start); }
