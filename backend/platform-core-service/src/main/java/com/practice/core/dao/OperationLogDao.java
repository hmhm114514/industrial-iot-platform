package com.practice.core.dao;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
    import java.time.LocalDateTime;
import java.util.List;

    public interface OperationLogDao extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> { List<OperationLog> findTop100ByOrderByCreatedAtDesc(); }
