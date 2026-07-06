package com.practice.visual.init;

import com.practice.visual.entity.DashboardScreen;
import com.practice.visual.entity.VideoAlarmTask;
import com.practice.visual.entity.VideoDevice;
import com.practice.visual.entity.VideoStreamProxy;
import com.practice.visual.repository.DashboardScreenRepo;
import com.practice.visual.repository.VideoAlarmTaskRepo;
import com.practice.visual.repository.VideoDeviceRepo;
import com.practice.visual.repository.VideoStreamProxyRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final DashboardScreenRepo screens;
    private final VideoDeviceRepo videos;
    private final VideoStreamProxyRepo streams;
    private final VideoAlarmTaskRepo tasks;

    public DataInitializer(
        DashboardScreenRepo screens,
        VideoDeviceRepo videos,
        VideoStreamProxyRepo streams,
        VideoAlarmTaskRepo tasks
    ) {
        this.screens = screens;
        this.videos = videos;
        this.streams = streams;
        this.tasks = tasks;
    }

    @Override
    public void run(String... args) {
        if (screens.count() > 0 || videos.count() > 0) return;

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

        VideoDevice device = new VideoDevice();
        device.name = "车间球机01";
        device.code = "CAM-001";
        device.channelNo = "CH-01";
        device.streamUrl = "";
        device.status = "OFFLINE";
        device.location = "一号车间";
        videos.save(device);

        VideoStreamProxy stream = new VideoStreamProxy();
        stream.name = "车间球机01-播放源";
        stream.code = "STREAM-001";
        stream.videoDeviceId = device.id;
        stream.protocol = "未配置";
        stream.playUrl = "";
        streams.save(stream);

        VideoAlarmTask task = new VideoAlarmTask();
        task.name = "安全帽识别任务";
        task.code = "AI-HELMET-001";
        task.videoDeviceId = device.id;
        task.algorithm = "helmet-detect";
        task.enabled = true;
        tasks.save(task);
    }
}
