package com.practice.core.dao;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.time.LocalDateTime;
import java.util.List;

    public interface RuleAuditDao extends JpaRepository<RuleAudit, Long> { List<RuleAudit> findTop50ByOrderByCreatedAtDesc(); }
