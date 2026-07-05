package com.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.*;

@Service
class PlatformService {
    final DeviceRepo devices; final ProductRepo products; final DeviceGroupRepo groups; final TelemetryRepo telemetry; final RuleRepo rules; final AlarmRepo alarms; final RuleAuditRepo audits; final TaskJobRepo tasks; final TaskLogRepo taskLogs; final OperationLogRepo opLogs; final UserRepo users; final LoginLogRepo loginLogs; final AiAgentRepo aiAgents;
    final ObjectMapper mapper = new ObjectMapper();
    @Value("${app.ai.enabled:true}") boolean aiEnabled;
    @Value("${app.ai.base-url:https://maas.icompify.com:32788/v1}") String aiBaseUrl;
    @Value("${app.ai.model:deepseek-v4-flash}") String aiModel;
    @Value("${app.ai.api-key:}") String aiApiKey;
    @Value("${app.ai.timeout-ms:25000}") long aiTimeoutMs;
    @Value("${app.ai.allow-local-key-file:false}") boolean aiAllowLocalKeyFile;
    PlatformService(DeviceRepo devices, ProductRepo products, DeviceGroupRepo groups, TelemetryRepo telemetry, RuleRepo rules, AlarmRepo alarms, RuleAuditRepo audits, TaskJobRepo tasks, TaskLogRepo taskLogs, OperationLogRepo opLogs, UserRepo users, LoginLogRepo loginLogs, AiAgentRepo aiAgents) {
        this.devices=devices; this.products=products; this.groups=groups; this.telemetry=telemetry; this.rules=rules; this.alarms=alarms; this.audits=audits; this.tasks=tasks; this.taskLogs=taskLogs; this.opLogs=opLogs; this.users=users; this.loginLogs=loginLogs; this.aiAgents=aiAgents;
    }
    @Transactional public Map<String,Object> login(String username, String password, String token, String ip) {
        SysUser u = users.findByUsername(username); boolean ok = u != null && Objects.equals(u.password, password);
        LoginLog log = new LoginLog(); log.name="登录-"+username; log.username=username; log.ip=ip; log.success=ok; loginLogs.save(log);
        if (!ok) throw new RuntimeException("用户名或密码错误");
        op("系统设置", "登录", username, "用户登录成功");
        Map<String,Object> user = new LinkedHashMap<>();
        user.put("id", u.id); user.put("username", u.username); user.put("name", u.name);
        user.put("realName", u.realName == null ? username : u.realName);
        user.put("role", u.roleName == null || u.roleName.isBlank() ? "" : u.roleName);
        user.put("roleName", u.roleName == null || u.roleName.isBlank() ? "" : u.roleName);
        user.put("status", u.status); user.put("createdAt", u.createdAt); user.put("updatedAt", u.updatedAt);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("token", token); result.put("tokenType", "Bearer");
        result.put("username", username); result.put("realName", user.get("realName")); result.put("role", user.get("role")); result.put("roleName", user.get("roleName"));
        result.put("user", user);
        return result;
    }
    @Transactional public Map<String,Object> simulate(Map<String,Object> req) {
        Long deviceId = Long.valueOf(String.valueOf(req.getOrDefault("deviceId", 1)));
        Device d = devices.findById(deviceId).orElseThrow(() -> new RuntimeException("设备不存在"));
        TelemetryData t = new TelemetryData(); t.deviceId=deviceId; t.deviceName=d.name;
        t.temperature=num(req.get("temperature")); t.humidity=num(req.get("humidity")); t.pressure=num(req.get("pressure")); t.payload=req.toString(); telemetry.save(t);
        d.status="ONLINE"; d.lastOnlineAt=LocalDateTime.now(); devices.save(d);
        List<Alarm> created = new ArrayList<>();
        for (Rule r: rules.findByEnabledTrue()) {
            Double v = switch (r.metric) { case "humidity" -> t.humidity; case "pressure" -> t.pressure; default -> t.temperature; };
            boolean hit = v != null && compare(v, r.operator, r.threshold);
            RuleAudit a = new RuleAudit(); a.name="规则审计-"+r.name; a.ruleId=r.id; a.deviceId=deviceId; a.status=hit?"HIT":"PASS"; a.result=a.status; a.detail=r.metric+"="+v+" "+r.operator+" "+r.threshold; audits.save(a);
            if (hit) { Alarm alarm = new Alarm(); alarm.name="温度阈值告警-"+d.name; alarm.code="ALM-"+System.currentTimeMillis(); alarm.status="OPEN"; alarm.deviceId=deviceId; alarm.deviceName=d.name; alarm.level=r.alarmLevel; alarm.type="THRESHOLD"; alarm.content=a.detail+"，触发规则："+r.name; alarms.save(alarm); created.add(alarm); }
        }
        op("数据中心", "模拟上报", "admin", "设备"+d.name+"上报遥测数据");
        return Map.of("telemetry", t, "alarms", created);
    }
    public Map<String,Object> summary() { return Map.of("products", products.count(), "groups", groups.count(), "devices", devices.count(), "onlineDevices", devices.countByStatus("ONLINE"), "alarms", alarms.count(), "openAlarms", alarms.countByStatus("OPEN"), "telemetryToday", telemetry.countByReportTimeAfter(LocalDate.now().atStartOfDay()), "tasks", tasks.count()); }
    public Map<String,Object> charts() {
        List<Device> ds = devices.findAll(); Map<String,Long> byStatus = ds.stream().collect(Collectors.groupingBy(x -> Optional.ofNullable(x.status).orElse("UNKNOWN"), Collectors.counting()));
        List<Alarm> as = alarms.findAll(); Map<String,Long> byLevel = as.stream().collect(Collectors.groupingBy(x -> Optional.ofNullable(x.level).orElse("INFO"), Collectors.counting()));
        List<LocalDate> dayDates = IntStream.rangeClosed(0,6).mapToObj(i -> LocalDate.now().minusDays(6-i)).toList();
        List<String> days = dayDates.stream().map(LocalDate::toString).toList();
        LocalDateTime windowStart = dayDates.get(0).atStartOfDay().minusNanos(1);
        Map<LocalDate, Long> messageCounts = telemetry.findByReportTimeAfter(windowStart).stream()
            .filter(x -> x.reportTime != null)
            .collect(Collectors.groupingBy(x -> x.reportTime.toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> deviceCounts = devices.findByCreatedAtAfter(windowStart).stream()
            .filter(x -> x.createdAt != null)
            .collect(Collectors.groupingBy(x -> x.createdAt.toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> alarmCounts = alarms.findByCreatedAtAfter(windowStart).stream()
            .filter(x -> x.createdAt != null)
            .collect(Collectors.groupingBy(x -> x.createdAt.toLocalDate(), Collectors.counting()));
        List<Map<String,Object>> messageTrend = trend(dayDates, messageCounts);
        List<Map<String,Object>> deviceGrowth = trend(dayDates, deviceCounts);
        List<Map<String,Object>> alarmTrend = trend(dayDates, alarmCounts);
        return Map.of("deviceStatus", byStatus, "alarmLevel", byLevel, "days", days, "messageTrend", messageTrend, "deviceGrowth", deviceGrowth, "alarmTrend", alarmTrend, "recentTelemetry", telemetry.findTop50ByOrderByReportTimeDesc(), "recentAlarms", alarms.findTop20ByOrderByCreatedAtDesc());
    }
    @Transactional public Alarm handleAlarm(Long id, Map<String,Object> body) { Alarm a=alarms.findById(id).orElseThrow(); a.status="CLOSED"; a.handler=String.valueOf(body.getOrDefault("handler","admin")); a.remark=String.valueOf(body.getOrDefault("remark","已处置")); a.handledAt=LocalDateTime.now(); op("规则引擎","处置告警",a.handler,a.name); return alarms.save(a); }
    @Transactional public TaskJob switchTask(Long id, boolean running) { TaskJob t=tasks.findById(id).orElseThrow(); t.running=running; t.status=running?"RUNNING":"STOPPED"; t.lastResult=running?"任务已启动，等待调度":"任务已停止"; tasks.save(t); TaskLog l=new TaskLog(); l.name="任务日志-"+t.name; l.taskId=id; l.status="SUCCESS"; l.result=t.lastResult; l.detail="模拟执行策略："+t.cron; taskLogs.save(l); return t; }
    @Transactional public void op(String module, String action, String operator, String detail) { OperationLog l=new OperationLog(); l.name=module+"-"+action; l.moduleName=module; l.action=action; l.operator=operator; l.detail=detail; opLogs.save(l); }
    public Map<String,Object> aiChat(Map<String,Object> req) {
        String agentCode = str(req.get("agentCode")); String message = str(req.get("message")); String context = str(req.get("context"));
        AiAgent agent = agentCode.isBlank() ? null : aiAgents.findByCode(agentCode);
        String requestedModel = str(req.get("model"));
        String model = !requestedModel.isBlank() ? requestedModel : (agent != null && agent.modelName != null && !agent.modelName.isBlank() ? agent.modelName : aiModel);
        if (model == null || model.isBlank()) model = "deepseek-v4-flash";
        if (agent != null && (Boolean.FALSE.equals(agent.enabled) || "DISABLED".equals(agent.status))) return demoAi(agent, message, model, "该智能体暂未启用，已切换为本地演示建议");
        if (!aiEnabled) return demoAi(agent, message, model, "智能服务暂未启用，已切换为本地演示建议");
        String key = resolveAiKey(req);
        if (key.isBlank()) return demoAi(agent, message, model, "智能服务配置待完善，已切换为本地演示建议");
        try {
            String base = Optional.ofNullable(str(req.get("baseUrl")).isBlank() ? aiBaseUrl : str(req.get("baseUrl"))).orElse("https://maas.icompify.com:32788/v1").replaceAll("/+$", "");
            String system = agent != null && agent.systemPrompt != null && !agent.systemPrompt.isBlank() ? agent.systemPrompt : "你是工业互联网平台的智能助手，请用中文给出简洁、可执行的建议。";
            List<Map<String,String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", system));
            if (!context.isBlank()) messages.add(Map.of("role", "user", "content", "业务上下文：" + context));
            messages.add(Map.of("role", "user", "content", message.isBlank() ? "请结合工业物联网场景给出建议" : message));
            Map<String,Object> payload = new LinkedHashMap<>();
            payload.put("model", model); payload.put("messages", messages); payload.put("stream", false); payload.put("max_tokens", 500); payload.put("temperature", 0.3);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(aiTimeoutMs)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/chat/completions"))
                .timeout(Duration.ofMillis(aiTimeoutMs))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) return demoAi(agent, message, model, "智能服务响应较慢，已切换为本地演示建议");
            JsonNode content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            String reply = content.asText("").trim();
            if (reply.isBlank()) return demoAi(agent, message, model, "智能服务响应较慢，已切换为本地演示建议");
            return Map.of("reply", reply, "source", "LIVE", "model", model, "notice", "智能服务已生成建议");
        } catch (Exception ignored) {
            return demoAi(agent, message, model, "智能服务响应较慢，已切换为本地演示建议");
        }
    }
    public Map<String,Object> aiModels(Map<String,Object> req) {
        String key = resolveAiKey(req);
        String base = Optional.ofNullable(str(req.get("baseUrl")).isBlank() ? aiBaseUrl : str(req.get("baseUrl"))).orElse("https://maas.icompify.com:32788/v1").replaceAll("/+$", "");
        if (!aiEnabled) return Map.of("source", "DEMO", "models", List.of("deepseek-v4-flash"), "notice", "智能服务暂未启用，当前显示默认模型");
        if (key.isBlank()) return Map.of("source", "DEMO", "models", List.of("deepseek-v4-flash"), "notice", "请填写 API Key，或使用本机示例配置");
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Math.min(aiTimeoutMs, 10000))).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/models"))
                .timeout(Duration.ofMillis(Math.min(aiTimeoutMs, 15000)))
                .header("Authorization", "Bearer " + key)
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) return Map.of("source", "DEMO", "models", List.of("deepseek-v4-flash"), "notice", "智能服务响应较慢，当前显示默认模型");
            JsonNode data = mapper.readTree(response.body()).path("data");
            List<String> models = new ArrayList<>();
            if (data.isArray()) data.forEach(x -> { String id = x.path("id").asText(""); if (!id.isBlank()) models.add(id); });
            if (models.isEmpty()) models.add("deepseek-v4-flash");
            return Map.of("source", "LIVE", "models", models, "notice", "已读取可用模型");
        } catch (Exception ignored) {
            return Map.of("source", "DEMO", "models", List.of("deepseek-v4-flash"), "notice", "智能服务响应较慢，当前显示默认模型");
        }
    }
    Map<String,Object> demoAi(AiAgent agent, String message, String model, String notice) {
        String scenario = agent == null || agent.scenario == null ? "工业物联网" : agent.scenario;
        String reply = "【" + scenario + "】本地演示建议：先确认设备在线状态与最近遥测趋势，再核对告警等级、规则阈值和现场处置记录；如问题持续，建议安排巡检并保留处理结论。";
        if (message != null && message.contains("巡检")) reply = "【巡检计划】本地演示建议：按设备重要度分组，优先检查离线设备、高频告警设备和关键网关，记录温度、湿度、压力及现场照片，完成后复核未关闭告警。";
        else if (message != null && message.contains("告警")) reply = "【告警解释】本地演示建议：结合触发规则、遥测峰值和设备位置判断影响范围，先降载或停机保护，再通知值班人员确认传感器与工况是否异常。";
        else if (message != null && (message.contains("状态") || message.contains("分析"))) reply = "【设备状态分析】本地演示建议：关注在线率、最近上报时间、告警数量和任务执行结果；若设备长时间无上报，优先检查供电、网络与边缘网关连接。";
        return Map.of("reply", reply, "source", "DEMO", "model", model == null ? "deepseek-v4-flash" : model, "notice", notice);
    }
    String resolveAiKey(Map<String,Object> req) {
        String requestKey = str(req.get("apiKey"));
        if (!requestKey.isBlank()) return requestKey;
        String key = Optional.ofNullable(aiApiKey).orElse("").trim();
        if (!key.isBlank() || !aiAllowLocalKeyFile) return key;
        try {
            Path cwd = Path.of("").toAbsolutePath(); String fileName = "接口文档和API-KEY.txt";
            List<Path> candidates = new ArrayList<>();
            if (cwd.getParent() != null) candidates.add(cwd.getParent().resolve(fileName));
            if (cwd.getParent() != null && cwd.getParent().getParent() != null) candidates.add(cwd.getParent().getParent().resolve(fileName));
            Pattern p = Pattern.compile("API-KEY\\s*[:：]\\s*([^\\s]+)");
            for (Path path: candidates) if (Files.isRegularFile(path)) { Matcher m = p.matcher(Files.readString(path, StandardCharsets.UTF_8)); if (m.find()) return m.group(1).trim(); }
        } catch (Exception ignored) {}
        return "";
    }
    static String str(Object o){ return o==null?"":String.valueOf(o).trim(); }
    static Double num(Object o){ return o==null?null:Double.valueOf(String.valueOf(o)); }
    static boolean compare(double v, String op, double th){ return switch(op){ case ">=" -> v>=th; case "<" -> v<th; case "<=" -> v<=th; case "==" -> v==th; default -> v>th; }; }
    static List<Map<String,Object>> trend(List<LocalDate> days, Map<LocalDate, Long> counts){ return days.stream().map(d -> Map.<String,Object>of("name", String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth()), "value", counts.getOrDefault(d, 0L))).toList(); }
    static <T> List<T> search(JpaRepository<T,Long> repo, String keyword) { List<T> all=repo.findAll(); if (keyword==null || keyword.isBlank()) return all; String kw=keyword.toLowerCase(); return all.stream().filter(x -> text(x).contains(kw)).toList(); }
    static String text(Object x){ StringBuilder sb=new StringBuilder(); for(Class<?> c=x.getClass(); c!=null; c=c.getSuperclass()) for(Field f:c.getDeclaredFields()) try{f.setAccessible(true); Object v=f.get(x); if(v!=null) sb.append(v).append(' ');}catch(Exception ignored){} return sb.toString().toLowerCase(); }
    static <T> T merge(T old, T body){ for(Class<?> c=body.getClass(); c!=null; c=c.getSuperclass()) for(Field f:c.getDeclaredFields()) try{ f.setAccessible(true); if("id".equals(f.getName())||"createdAt".equals(f.getName())) continue; Object v=f.get(body); if(v!=null) f.set(old,v); }catch(Exception ignored){} return old; }
}
