package com.practice.core.repository;
import com.practice.core.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskJobRepo extends JpaRepository<TaskJob, Long> {  }
