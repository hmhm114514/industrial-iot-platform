package com.practice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@org.springframework.stereotype.Component
class DataInitializer implements CommandLineRunner {
    final RoleRepo roles; final UserRepo users; final ProductCategoryRepo categories; final ProductRepo products; final DeviceGroupRepo groups; final DeviceRepo devices; final RuleRepo rules; final NetworkServiceRepo networks; final ParseScriptRepo scripts; final DashboardScreenRepo screens; final VideoDeviceRepo videos; final VideoStreamProxyRepo streams; final VideoAlarmTaskRepo videoTasks; final TaskJobRepo tasks; final FirmwareRepo firmwares; final AiAgentRepo aiAgents; final PlatformService service;
    DataInitializer(RoleRepo roles, UserRepo users, ProductCategoryRepo categories, ProductRepo products, DeviceGroupRepo groups, DeviceRepo devices, RuleRepo rules, NetworkServiceRepo networks, ParseScriptRepo scripts, DashboardScreenRepo screens, VideoDeviceRepo videos, VideoStreamProxyRepo streams, VideoAlarmTaskRepo videoTasks, TaskJobRepo tasks, FirmwareRepo firmwares, AiAgentRepo aiAgents, PlatformService service) { this.roles=roles; this.users=users; this.categories=categories; this.products=products; this.groups=groups; this.devices=devices; this.rules=rules; this.networks=networks; this.scripts=scripts; this.screens=screens; this.videos=videos; this.streams=streams; this.videoTasks=videoTasks; this.tasks=tasks; this.firmwares=firmwares; this.aiAgents=aiAgents; this.service=service; }
    @Override public void run(String... args) {
        boolean hadUsers = users.count() > 0;
        seedCoreAccounts();
        if (hadUsers) { seedAiAgents(); return; }
        ProductCategory c1=new ProductCategory(); c1.name="工业传感器"; c1.code="sensor"; categories.save(c1); ProductCategory c2=new ProductCategory(); c2.name="边缘网关"; c2.code="gateway"; categories.save(c2);
        Product p1=new Product(); p1.name="PandaX温湿度采集器"; p1.code="PDX-TH-100"; p1.categoryId=c1.id; p1.protocol="MQTT"; p1.manufacturer="PandaX实验室"; products.save(p1);
        Product p2=new Product(); p2.name="PandaX边缘网关"; p2.code="PDX-GW-200"; p2.categoryId=c2.id; p2.protocol="MQTT/HTTP"; p2.manufacturer="PandaX实验室"; products.save(p2);
        DeviceGroup g=new DeviceGroup(); g.name="一号车间"; g.code="workshop-1"; groups.save(g); DeviceGroup g2=new DeviceGroup(); g2.name="能源站"; g2.code="energy-station"; groups.save(g2);
        Device d=new Device(); d.name="注塑机温度传感器01"; d.code="DEV-TH-001"; d.deviceKey="key-th-001"; d.productId=p1.id; d.groupId=g.id; d.status="ONLINE"; d.location="一号车间A区"; d.latitude=31.2304; d.longitude=121.4737; d.lastOnlineAt= LocalDateTime.now(); devices.save(d);
        Device d2=new Device(); d2.name="空压站边缘网关01"; d2.code="DEV-GW-001"; d2.deviceKey="key-gw-001"; d2.productId=p2.id; d2.groupId=g2.id; d2.status="OFFLINE"; d2.location="能源站B区"; d2.latitude=31.2360; d2.longitude=121.4860; devices.save(d2);
        Rule r=new Rule(); r.name="温度超过80℃自动告警"; r.code="RULE-TEMP-80"; r.metric="temperature"; r.operator=">"; r.threshold=80.0; r.enabled=true; r.alarmLevel="HIGH"; rules.save(r);
        NetworkService ns=new NetworkService(); ns.name="MQTT接入服务"; ns.code="mqtt-server"; ns.type="MQTT"; ns.host="0.0.0.0"; ns.port=1883; ns.status="RUNNING"; ns.upMessages=1280L; ns.downMessages=640L; networks.save(ns);
        NetworkService http=new NetworkService(); http.name="HTTP数据接入服务"; http.code="http-ingest"; http.type="HTTP"; http.host="0.0.0.0"; http.port=8088; http.status="STOPPED"; networks.save(http);
        ParseScript sc=new ParseScript(); sc.name="温湿度JSON解析脚本"; sc.code="parse-json-th"; sc.script="return {temperature: payload.temp, humidity: payload.hum};"; scripts.save(sc);
        DashboardScreen screen=new DashboardScreen(); screen.name="PandaX工厂运行总览"; screen.code="screen-factory"; screen.groupName="生产看板"; screen.published=true; screen.status="PUBLISHED"; screen.configJson="{\"theme\":\"dark\",\"widgets\":[{\"id\":\"device-stat\",\"type\":\"stat\",\"title\":\"设备总数\",\"metric\":\"devices\"},{\"id\":\"online-stat\",\"type\":\"stat\",\"title\":\"在线设备\",\"metric\":\"onlineDevices\"},{\"id\":\"alarm-stat\",\"type\":\"stat\",\"title\":\"当前告警\",\"metric\":\"alarms\",\"tone\":\"warn\"},{\"id\":\"message-trend\",\"type\":\"trend\",\"title\":\"消息趋势\",\"metric\":\"messageTrend\",\"span\":2},{\"id\":\"device-list\",\"type\":\"list\",\"title\":\"设备运行状态\",\"metric\":\"devices\"},{\"id\":\"topology\",\"type\":\"topology\",\"title\":\"设备拓扑\",\"metric\":\"topology\",\"span\":2}]}"; screens.save(screen);
        VideoDevice vd=new VideoDevice(); vd.name="车间球机01"; vd.code="CAM-001"; vd.channelNo="CH-01"; vd.streamUrl="rtsp://demo.pandax.local/live/001"; vd.status="ONLINE"; videos.save(vd);
        VideoStreamProxy sp=new VideoStreamProxy(); sp.name="车间球机01-FLV代理"; sp.code="STREAM-001"; sp.videoDeviceId=vd.id; sp.protocol="FLV"; sp.playUrl="http://localhost:8080/mock/live/001.flv"; streams.save(sp);
        VideoAlarmTask vat=new VideoAlarmTask(); vat.name="安全帽识别任务"; vat.code="AI-HELMET-001"; vat.videoDeviceId=vd.id; vat.algorithm="helmet-detect"; vat.enabled=true; videoTasks.save(vat);
        TaskJob job=new TaskJob(); job.name="设备在线状态巡检"; job.code="TASK-ONLINE-CHECK"; job.cron="0 */5 * * * ?"; job.running=true; job.status="RUNNING"; tasks.save(job);
        Firmware fw=new Firmware(); fw.name="PandaX网关固件"; fw.code="FW-GW-1.0.0"; fw.version="v1.0.0"; fw.targetProduct="PandaX边缘网关"; fw.fileUrl="/files/fw-gw-1.0.0.bin"; firmwares.save(fw);
        service.simulate(java.util.Map.of("deviceId", d.id, "temperature", 76.5, "humidity", 45.2, "pressure", 101.3));
        service.simulate(java.util.Map.of("deviceId", d.id, "temperature", 86.2, "humidity", 48.7, "pressure", 101.1));
        seedAiAgents();
    }
    void seedCoreAccounts() {
        ensureRole("超级管理员", "admin", "*:*:*");
        ensureRole("平台管理员", "manager", "platform:*:*");
        ensureUser("admin", "123456", "PandaX管理员", "超级管理员");
        ensureUser("manager", "123456", "平台管理员", "平台管理员");
    }
    void ensureRole(String name, String code, String permissions) {
        Role role = roles.findAll().stream()
            .filter(x -> name.equals(x.name) || code.equals(x.code))
            .findFirst().orElse(null);
        if (role == null) { role = new Role(); role.name=name; role.code=code; }
        if (role.name == null || role.name.isBlank()) role.name = name;
        if (role.code == null || role.code.isBlank()) role.code = code;
        if (role.permissions == null || role.permissions.isBlank()) role.permissions = permissions;
        roles.save(role);
    }
    void ensureUser(String username, String password, String realName, String roleName) {
        SysUser user = users.findByUsername(username);
        if (user == null) { user = new SysUser(); user.username=username; user.password=password; }
        if (user.name == null || user.name.isBlank()) user.name = realName;
        user.password = password;
        if (user.realName == null || user.realName.isBlank()) user.realName = realName;
        user.roleName = roleName;
        users.save(user);
    }
    void seedAiAgents() {
        if (aiAgents.findByCode("alarm-explain") == null) { AiAgent a=new AiAgent(); a.name="告警解释助手"; a.code="alarm-explain"; a.scenario="告警解释"; a.description="解释工业设备告警原因、影响范围和处置建议"; a.systemPrompt="你是工业互联网平台的告警解释助手，请结合设备、规则和遥测信息，用中文给出原因分析、影响范围和处置步骤。"; a.modelName="deepseek-v4-flash"; a.status="ENABLED"; a.enabled=true; aiAgents.save(a); }
        if (aiAgents.findByCode("inspection-plan") == null) { AiAgent a=new AiAgent(); a.name="巡检计划助手"; a.code="inspection-plan"; a.scenario="巡检计划"; a.description="根据设备状态、告警和任务情况生成巡检重点"; a.systemPrompt="你是工业互联网平台的巡检计划助手，请按优先级输出巡检对象、检查项、风险点和记录要求。"; a.modelName="deepseek-v4-flash"; a.status="ENABLED"; a.enabled=true; aiAgents.save(a); }
        if (aiAgents.findByCode("device-status") == null) { AiAgent a=new AiAgent(); a.name="设备状态分析助手"; a.code="device-status"; a.scenario="设备状态分析"; a.description="分析设备在线、遥测趋势和运行健康度"; a.systemPrompt="你是工业互联网平台的设备状态分析助手，请围绕在线状态、最近遥测、告警数量和任务结果给出健康判断与建议。"; a.modelName="deepseek-v4-flash"; a.status="ENABLED"; a.enabled=true; aiAgents.save(a); }
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.OK) ApiResponse<Object> handle(Exception e){ return ApiResponse.fail(e.getMessage()==null?"服务异常":e.getMessage()); }
}
