package com.practice.core.entity;
    import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.practice.common.BaseEntity;

    @Entity
    public class ParseScript extends BaseEntity { @Column(length=4000) public String script; public String language = "JavaScript"; }
