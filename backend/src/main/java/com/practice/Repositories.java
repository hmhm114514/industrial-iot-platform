package com.practice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

interface RoleRepo extends JpaRepository<Role, Long> {}
interface UserRepo extends JpaRepository<SysUser, Long> { SysUser findByUsername(String username); }
interface ProductCategoryRepo extends JpaRepository<ProductCategory, Long> {}
interface ProductRepo extends JpaRepository<Product, Long> {}
interface DeviceGroupRepo extends JpaRepository<DeviceGroup, Long> {}
interface DeviceRepo extends JpaRepository<Device, Long> { long countByStatus(String status); List<Device> findByCreatedAtAfter(LocalDateTime start); }
interface TelemetryRepo extends JpaRepository<TelemetryData, Long> { List<TelemetryData> findTop50ByOrderByReportTimeDesc(); long countByReportTimeAfter(LocalDateTime time); List<TelemetryData> findByReportTimeAfter(LocalDateTime start); }
interface AlarmRepo extends JpaRepository<Alarm, Long> { long countByStatus(String status); List<Alarm> findTop20ByOrderByCreatedAtDesc(); List<Alarm> findByCreatedAtAfter(LocalDateTime start); }
interface RuleRepo extends JpaRepository<Rule, Long> { List<Rule> findByEnabledTrue(); }
interface RuleAuditRepo extends JpaRepository<RuleAudit, Long> { List<RuleAudit> findTop50ByOrderByCreatedAtDesc(); }
interface NetworkServiceRepo extends JpaRepository<NetworkService, Long> {}
interface ParseScriptRepo extends JpaRepository<ParseScript, Long> {}
interface DashboardScreenRepo extends JpaRepository<DashboardScreen, Long> {}
interface VideoDeviceRepo extends JpaRepository<VideoDevice, Long> {}
interface VideoStreamProxyRepo extends JpaRepository<VideoStreamProxy, Long> {}
interface VideoAlarmTaskRepo extends JpaRepository<VideoAlarmTask, Long> {}
interface TaskJobRepo extends JpaRepository<TaskJob, Long> {}
interface TaskLogRepo extends JpaRepository<TaskLog, Long> { List<TaskLog> findTop50ByOrderByExecuteTimeDesc(); }
interface FirmwareRepo extends JpaRepository<Firmware, Long> {}
interface AiAgentRepo extends JpaRepository<AiAgent, Long> { AiAgent findByCode(String code); }
interface OperationLogRepo extends JpaRepository<OperationLog, Long> { List<OperationLog> findTop100ByOrderByCreatedAtDesc(); }
interface LoginLogRepo extends JpaRepository<LoginLog, Long> {}
