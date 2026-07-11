package com.practice.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.core.dao.NetworkServiceDao;
import com.practice.core.entity.NetworkService;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class MqttTelemetrySubscriber {
    private final PlatformCoreService service;
    private final NetworkServiceDao networkServices;
    private final ObjectMapper mapper = new ObjectMapper();
    private MqttClient client;
    private boolean subscribed = false;
    @Value("${app.mqtt.enabled:true}") boolean enabled;
    @Value("${app.mqtt.broker-url:tcp://localhost:1883}") String brokerUrl;
    @Value("${app.mqtt.client-id:pandax-platform-core}") String clientId;
    @Value("${app.mqtt.topic:/iot/+/telemetry}") String topic;
    @Value("${app.mqtt.fallback-topic:/iot/telemetry}") String fallbackTopic;
    @Value("${app.mqtt.username:}") String username;
    @Value("${app.mqtt.password:}") String password;

    public MqttTelemetrySubscriber(PlatformCoreService service, NetworkServiceDao networkServices) {
        this.service = service;
        this.networkServices = networkServices;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!enabled) return;
        try {
            client = new MqttClient(brokerUrl, clientId + "-" + UUID.randomUUID(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(5);
            if (!username.isBlank()) options.setUserName(username);
            if (!password.isBlank()) options.setPassword(password.toCharArray());
            client.setCallback(new MqttCallback() {
                @Override public void connectionLost(Throwable cause) {}
                @Override public void deliveryComplete(IMqttDeliveryToken token) {}
                @Override public void messageArrived(String messageTopic, MqttMessage message) {
                    handleMessage(messageTopic, message);
                }
            });
            client.connect(options);
            applyNetworkServiceStatus(mqttServiceRunning() ? "RUNNING" : "STOPPED");
            System.out.println("MQTT telemetry subscriber connected: " + brokerUrl + ", topic=" + topic + ", subscribed=" + subscribed);
        } catch (Exception ex) {
            System.out.println("MQTT telemetry subscriber not connected: " + ex.getMessage());
        }
    }

    public synchronized void applyNetworkServiceStatus(String status) {
        if (client == null || !client.isConnected()) return;
        try {
            if ("RUNNING".equals(status) && !subscribed) {
                client.subscribe(topic, 1);
                if (!fallbackTopic.isBlank() && !fallbackTopic.equals(topic)) client.subscribe(fallbackTopic, 1);
                subscribed = true;
                System.out.println("MQTT telemetry subscriber enabled: topic=" + topic);
            } else if (!"RUNNING".equals(status) && subscribed) {
                client.unsubscribe(topic);
                if (!fallbackTopic.isBlank() && !fallbackTopic.equals(topic)) client.unsubscribe(fallbackTopic);
                subscribed = false;
                System.out.println("MQTT telemetry subscriber disabled: network service mqtt-server is " + status);
            }
        } catch (Exception ex) {
            System.out.println("MQTT telemetry subscription update failed: " + ex.getMessage());
        }
    }

    void handleMessage(String messageTopic, MqttMessage message) {
        try {
            String text = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("MQTT telemetry received: topic=" + messageTopic + ", payload=" + text);
            if (!mqttServiceRunning()) {
                System.out.println("MQTT telemetry rejected: network service mqtt-server is not RUNNING");
                return;
            }
            Map<String, Object> payload = mapper.readValue(text, new TypeReference<>() {});
            String topicDeviceKey = deviceKeyFromTopic(messageTopic);
            if (!topicDeviceKey.isBlank() && !payload.containsKey("deviceKey")) payload.put("deviceKey", topicDeviceKey);
            payload.putIfAbsent("source", "mqtt");
            payload.put("mqttTopic", messageTopic);
            Map<String, Object> result = service.report(payload);
            Object telemetry = result.get("telemetry");
            Object alarms = result.get("alarms");
            System.out.println("MQTT telemetry processed: topic=" + messageTopic + ", telemetry=" + telemetry + ", alarms=" + alarms);
        } catch (Exception ex) {
            System.out.println("MQTT telemetry message ignored: " + ex.getMessage());
        }
    }

    String deviceKeyFromTopic(String messageTopic) {
        if (messageTopic == null) return "";
        String[] parts = messageTopic.split("/");
        if (parts.length >= 4 && "iot".equals(parts[1]) && "telemetry".equals(parts[3])) return parts[2];
        return "";
    }

    boolean mqttServiceRunning() {
        NetworkService mqtt = networkServices.findByCode("mqtt-server");
        return mqtt == null || "RUNNING".equals(mqtt.status);
    }
}
