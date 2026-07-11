package com.practice.core.dao;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.time.LocalDateTime;
import java.util.List;

    public interface TaskLogDao extends JpaRepository<TaskLog, Long> { List<TaskLog> findTop50ByOrderByExecuteTimeDesc(); }
