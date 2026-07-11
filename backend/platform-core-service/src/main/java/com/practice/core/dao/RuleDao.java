package com.practice.core.dao;
    import com.practice.core.entity.*;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.time.LocalDateTime;
import java.util.List;

    public interface RuleDao extends JpaRepository<Rule, Long> { List<Rule> findByEnabledTrue(); }
