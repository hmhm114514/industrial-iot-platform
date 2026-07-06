package com.practice.core.repository;
import com.practice.core.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAgentRepo extends JpaRepository<AiAgent, Long> { AiAgent findByCode(String code); }
