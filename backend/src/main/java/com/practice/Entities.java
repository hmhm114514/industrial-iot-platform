package com.practice;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    public String code;
    public String status = "ENABLED";
    public String remark;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @PrePersist void prePersist(){ createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void preUpdate(){ updatedAt = LocalDateTime.now(); }
}

@Entity class Role extends BaseEntity { public String permissions; }
@Entity class SysUser extends BaseEntity { public String username; public String password; public String realName; public String roleName; }
@Entity class ProductCategory extends BaseEntity { public Long parentId; }
@Entity class Product extends BaseEntity { public Long categoryId; public String protocol; public String manufacturer; }
@Entity class DeviceGroup extends BaseEntity { public Long parentId; }
@Entity class Device extends BaseEntity { public Long productId; public Long groupId; public String deviceKey; public String location; public Double latitude; public Double longitude; public LocalDateTime lastOnlineAt; }

@Entity class TelemetryData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long deviceId; public String deviceName;
    public Double temperature; public Double humidity; public Double pressure;
    @Column(length=2000) public String payload;
    public LocalDateTime reportTime = LocalDateTime.now();
}

@Entity class Alarm extends BaseEntity { public Long deviceId; public String deviceName; public String level; public String type; public String content; public String handler; public LocalDateTime handledAt; }
@Entity class Rule extends BaseEntity { public String metric = "temperature"; public String operator = ">"; public Double threshold = 80.0; public Boolean enabled = true; public String alarmLevel = "HIGH"; }
@Entity class RuleAudit extends BaseEntity { public Long ruleId; public Long deviceId; public String result; public String detail; }
@Entity class NetworkService extends BaseEntity { public String type; public String host; public Integer port; public Long upMessages = 0L; public Long downMessages = 0L; }
@Entity class ParseScript extends BaseEntity { @Column(length=4000) public String script; public String language = "JavaScript"; }
@Entity class DashboardScreen extends BaseEntity { public String groupName; public Boolean published = false; @Column(length=4000) public String configJson; }
@Entity class VideoDevice extends BaseEntity { public String channelNo; public String streamUrl; public String location; }
@Entity class VideoStreamProxy extends BaseEntity { public Long videoDeviceId; public String playUrl; public String protocol; }
@Entity class VideoAlarmTask extends BaseEntity { public Long videoDeviceId; public String algorithm; public Boolean enabled = true; }
@Entity class TaskJob extends BaseEntity { public String cron; public Boolean running = false; public String lastResult; }
@Entity class TaskLog extends BaseEntity { public Long taskId; public String result; public String detail; public LocalDateTime executeTime = LocalDateTime.now(); }
@Entity class Firmware extends BaseEntity { public String version; public String targetProduct; public String upgradeStatus = "READY"; public String fileUrl; }
@Entity class AiAgent extends BaseEntity { public String scenario; @Column(length=1000) public String description; @Column(length=4000) public String systemPrompt; public String modelName; public Boolean enabled = true; }
@Entity class OperationLog extends BaseEntity { public String moduleName; public String action; public String operator; public String detail; }
@Entity class LoginLog extends BaseEntity { public String username; public String ip; public Boolean success; }
