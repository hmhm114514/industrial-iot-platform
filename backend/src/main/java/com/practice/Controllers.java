package com.practice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/auth") class AuthController { @Value("${app.token:panda-iot-demo-token}") String token; final PlatformService service; AuthController(PlatformService service){this.service=service;} @PostMapping("/login") ApiResponse<Map<String,Object>> login(@RequestBody Map<String,String> b, HttpServletRequest r){ return ApiResponse.ok(service.login(b.get("username"), b.get("password"), token, r.getRemoteAddr())); } }
@RestController @RequestMapping("/api/dashboard") class DashboardController { final PlatformService s; DashboardController(PlatformService s){this.s=s;} @GetMapping("/summary") ApiResponse<Map<String,Object>> summary(){return ApiResponse.ok(s.summary());} @GetMapping("/charts") ApiResponse<Map<String,Object>> charts(){return ApiResponse.ok(s.charts());} @GetMapping("/monitor") ApiResponse<Map<String,Object>> monitor(){return ApiResponse.ok(Map.of("cpu",42,"memory",63,"disk",58,"mqtt","running","http","running","jvmThreads",28));} }
@RestController @RequestMapping("/api/telemetry") class TelemetryController { final PlatformService s; final TelemetryRepo repo; TelemetryController(PlatformService s, TelemetryRepo repo){this.s=s;this.repo=repo;} @PostMapping("/simulate") ApiResponse<Map<String,Object>> simulate(@RequestBody Map<String,Object> b){return ApiResponse.ok(s.simulate(b));} @GetMapping ApiResponse<List<TelemetryData>> list(){return ApiResponse.ok(repo.findTop50ByOrderByReportTimeDesc());} }
@RestController @RequestMapping("/api/alarms") class AlarmController extends CrudController<Alarm>{ final PlatformService s; AlarmController(AlarmRepo r, PlatformService s){super(r);this.s=s;} @PostMapping("/{id}/handle") ApiResponse<Alarm> handle(@PathVariable Long id,@RequestBody Map<String,Object>b){return ApiResponse.ok(s.handleAlarm(id,b));} @PostMapping("/{id}/close") ApiResponse<Alarm> close(@PathVariable Long id){return ApiResponse.ok(s.handleAlarm(id,Map.of("remark","关闭告警")));} }
@RestController @RequestMapping("/api/tasks") class TaskController extends CrudController<TaskJob>{ final PlatformService s; final TaskLogRepo logs; TaskController(TaskJobRepo r, PlatformService s, TaskLogRepo logs){super(r);this.s=s;this.logs=logs;} @PostMapping("/{id}/start") ApiResponse<TaskJob> start(@PathVariable Long id){return ApiResponse.ok(s.switchTask(id,true));} @PostMapping("/{id}/stop") ApiResponse<TaskJob> stop(@PathVariable Long id){return ApiResponse.ok(s.switchTask(id,false));} @GetMapping("/logs") ApiResponse<List<TaskLog>> logs(){return ApiResponse.ok(logs.findTop50ByOrderByExecuteTimeDesc());} }
@RestController @RequestMapping("/api/network-services") class NetworkServiceController extends CrudController<NetworkService>{ NetworkServiceController(NetworkServiceRepo r){super(r);} @PostMapping("/{id}/start") ApiResponse<NetworkService> start(@PathVariable Long id){return status(id,"RUNNING");} @PostMapping("/{id}/stop") ApiResponse<NetworkService> stop(@PathVariable Long id){return status(id,"STOPPED");} }
@RestController @RequestMapping("/api/screens") class ScreenController extends CrudController<DashboardScreen>{ ScreenController(DashboardScreenRepo r){super(r);} @PostMapping("/{id}/publish") ApiResponse<DashboardScreen> pub(@PathVariable Long id){ DashboardScreen x=((DashboardScreenRepo)repo).findById(id).orElseThrow(); x.published=true; x.status="PUBLISHED"; return ApiResponse.ok(repo.save(x)); } }
@RestController @RequestMapping("/api/firmwares") class FirmwareController extends CrudController<Firmware>{ FirmwareController(FirmwareRepo r){super(r);} @PostMapping("/{id}/upgrade") ApiResponse<Firmware> upgrade(@PathVariable Long id,@RequestBody(required=false) Map<String,Object>b){ Firmware f=((FirmwareRepo)repo).findById(id).orElseThrow(); f.upgradeStatus= b==null?"UPGRADING":String.valueOf(b.getOrDefault("status","UPGRADING")); f.status=f.upgradeStatus; return ApiResponse.ok(repo.save(f)); } }
@RestController @RequestMapping("/api/ai-agents") class AiAgentController extends CrudController<AiAgent>{ final PlatformService s; AiAgentController(AiAgentRepo r, PlatformService s){super(r);this.s=s;} @PostMapping("/chat") ApiResponse<Map<String,Object>> chat(@RequestBody Map<String,Object> b){return ApiResponse.ok(s.aiChat(b));} @PostMapping("/models") ApiResponse<Map<String,Object>> models(@RequestBody(required=false) Map<String,Object> b){return ApiResponse.ok(s.aiModels(b==null?Map.of():b));} }

@RestController @RequestMapping("/api/users") class UserController extends CrudController<SysUser>{
    UserController(UserRepo r){super(r);}
    @Override @PostMapping ApiResponse<SysUser> create(@RequestBody SysUser body){ if (body.password == null || body.password.isBlank()) body.password="123456"; return ApiResponse.ok(repo.save(body)); }
    @Override @PutMapping("/{id}") ApiResponse<SysUser> update(@PathVariable Long id,@RequestBody SysUser body){ SysUser old=((UserRepo)repo).findById(id).orElseThrow(); if (body.password == null || body.password.isBlank()) body.password=null; return ApiResponse.ok(repo.save(PlatformService.merge(old,body))); }
}
@RestController @RequestMapping("/api/roles") class RoleController extends CrudController<Role>{ RoleController(RoleRepo r){super(r);} }
@RestController @RequestMapping("/api/product-categories") class ProductCategoryController extends CrudController<ProductCategory>{ ProductCategoryController(ProductCategoryRepo r){super(r);} }
@RestController @RequestMapping("/api/products") class ProductController extends CrudController<Product>{ ProductController(ProductRepo r){super(r);} }
@RestController @RequestMapping("/api/device-groups") class DeviceGroupController extends CrudController<DeviceGroup>{ DeviceGroupController(DeviceGroupRepo r){super(r);} }
@RestController @RequestMapping("/api/devices") class DeviceController extends CrudController<Device>{ DeviceController(DeviceRepo r){super(r);} }
@RestController @RequestMapping("/api/rules") class RuleController extends CrudController<Rule>{ RuleController(RuleRepo r){super(r);} }
@RestController @RequestMapping("/api/rule-audits") class RuleAuditController extends CrudController<RuleAudit>{ RuleAuditController(RuleAuditRepo r){super(r);} }
@RestController @RequestMapping("/api/scripts") class ScriptController extends CrudController<ParseScript>{ ScriptController(ParseScriptRepo r){super(r);} }
@RestController @RequestMapping("/api/video-devices") class VideoDeviceController extends CrudController<VideoDevice>{ VideoDeviceController(VideoDeviceRepo r){super(r);} }
@RestController @RequestMapping("/api/video-streams") class VideoStreamController extends CrudController<VideoStreamProxy>{ VideoStreamController(VideoStreamProxyRepo r){super(r);} }
@RestController @RequestMapping("/api/video-alarm-tasks") class VideoAlarmTaskController extends CrudController<VideoAlarmTask>{ VideoAlarmTaskController(VideoAlarmTaskRepo r){super(r);} }
@RestController @RequestMapping("/api/operation-logs") class OperationLogController extends CrudController<OperationLog>{ OperationLogController(OperationLogRepo r){super(r);} }
@RestController @RequestMapping("/api/login-logs") class LoginLogController extends CrudController<LoginLog>{ LoginLogController(LoginLogRepo r){super(r);} }

abstract class CrudController<T extends BaseEntity> {
    final JpaRepository<T,Long> repo; CrudController(JpaRepository<T,Long> repo){this.repo=repo;}
    @GetMapping ApiResponse<List<T>> list(@RequestParam(required=false) String keyword){ return ApiResponse.ok(PlatformService.search(repo, keyword)); }
    @GetMapping("/{id}") ApiResponse<T> get(@PathVariable Long id){ return ApiResponse.ok(repo.findById(id).orElseThrow()); }
    @PostMapping ApiResponse<T> create(@RequestBody T body){ return ApiResponse.ok(repo.save(body)); }
    @PutMapping("/{id}") ApiResponse<T> update(@PathVariable Long id,@RequestBody T body){ T old=repo.findById(id).orElseThrow(); return ApiResponse.ok(repo.save(PlatformService.merge(old,body))); }
    @DeleteMapping("/{id}") ApiResponse<Boolean> delete(@PathVariable Long id){ repo.deleteById(id); return ApiResponse.ok(true); }
    @PostMapping("/{id}/toggle") ApiResponse<T> toggle(@PathVariable Long id){
        T old=repo.findById(id).orElseThrow();
        boolean active = "ENABLED".equals(old.status)||"ONLINE".equals(old.status)||"RUNNING".equals(old.status);
        old.status = active ? "DISABLED" : "ENABLED";
        if (old instanceof Rule r) r.enabled = !active;
        if (old instanceof AiAgent a) a.enabled = !active;
        if (old instanceof TaskJob t) { t.running = !active; t.status = t.running ? "RUNNING" : "STOPPED"; }
        return ApiResponse.ok(repo.save(old));
    }
    ApiResponse<T> status(Long id,String status){ T old=repo.findById(id).orElseThrow(); old.status=status; return ApiResponse.ok(repo.save(old)); }
}
