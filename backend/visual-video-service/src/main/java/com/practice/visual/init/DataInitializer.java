package com.practice.visual.init;

import com.practice.visual.entity.DashboardScreen;
import com.practice.visual.dao.DashboardScreenDao;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final DashboardScreenDao screens;
    public DataInitializer(DashboardScreenDao screens) {
        this.screens = screens;
    }

    @Override
    public void run(String... args) {
        if (screens.count() > 0) return;

        DashboardScreen screen = new DashboardScreen();
        screen.name = "工厂运行总览";
        screen.code = "screen-factory";
        screen.groupName = "生产看板";
        screen.published = true;
        screen.status = "PUBLISHED";
        screen.configJson = "{\"theme\":\"dark\",\"widgets\":["
            + "{\"id\":\"device-stat\",\"type\":\"stat\",\"title\":\"设备总数\",\"metric\":\"devices\"},"
            + "{\"id\":\"online-stat\",\"type\":\"stat\",\"title\":\"在线设备\",\"metric\":\"onlineDevices\"},"
            + "{\"id\":\"alarm-stat\",\"type\":\"stat\",\"title\":\"当前告警\",\"metric\":\"alarms\",\"tone\":\"warn\"},"
            + "{\"id\":\"message-trend\",\"type\":\"trend\",\"title\":\"消息趋势\",\"metric\":\"messageTrend\",\"span\":2},"
            + "{\"id\":\"device-list\",\"type\":\"list\",\"title\":\"设备运行状态\",\"metric\":\"devices\"}"
            + "]}";
        screens.save(screen);
    }
}
