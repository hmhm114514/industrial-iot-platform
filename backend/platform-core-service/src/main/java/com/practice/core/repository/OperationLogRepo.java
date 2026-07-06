package com.practice.core.repository;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.time.LocalDateTime;
import java.util.List;

    public interface OperationLogRepo extends JpaRepository<OperationLog, Long> { List<OperationLog> findTop100ByOrderByCreatedAtDesc(); }
