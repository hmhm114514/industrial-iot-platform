package com.practice.visual;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.practice.visual.dao.VideoDeviceDao;
import com.practice.visual.entity.VideoDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalCameraControllerTest {
    private static final String BASE = "/api/video-devices/local-cameras";
    private static final String AUTH = "Bearer test-token";
    @Autowired MockMvc mvc;
    @Autowired VideoDeviceDao devices;

    @BeforeEach void clean() { devices.deleteAll(); }

    @Test void authenticationIsRequired() throws Exception { mvc.perform(get(BASE)).andExpect(status().isUnauthorized()); }

    @Test void createForcesTypeAndIgnoresLegacyFields() throws Exception {
        mvc.perform(post(BASE).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Camera", "CAM-1", "station-a", "browser-1", ",\"deviceType\":\"OTHER\",\"status\":\"ONLINE\",\"channelNo\":\"CH-X\",\"streamUrl\":\"rtsp://x\"")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.deviceType").value("LOCAL_CAMERA"))
            .andExpect(jsonPath("$.data.status").value("ENABLED")).andExpect(jsonPath("$.data.channelNo").isEmpty()).andExpect(jsonPath("$.data.streamUrl").isEmpty())
            .andExpect(jsonPath("$.data.browserDeviceHash").doesNotExist());
        String hash = devices.findAll().get(0).browserDeviceHash;
        org.junit.jupiter.api.Assertions.assertEquals(64, hash.length());
        org.junit.jupiter.api.Assertions.assertEquals("5e26d7146bd0f49c8b40a10b95dcdd2d5da9df87ea16d5bc6224c45a074193cc", hash);
    }

    @Test void fullRoundTrip() throws Exception {
        long id = create("Camera", "CAM-1", "station-a", "browser-1");
        mvc.perform(get(BASE + "/" + id).header("Authorization", AUTH)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Camera")).andExpect(jsonPath("$.data.purpose").value("inspection"))
            .andExpect(jsonPath("$.data.bindingClientId").value("station-a")).andExpect(jsonPath("$.data.browserDeviceId").value("browser-1"))
            .andExpect(jsonPath("$.data.createdAt").exists()).andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test void listAllAndFilterByBindingClient() throws Exception {
        create("One", "CAM-1", "station-a", "browser-1"); create("Two", "CAM-2", "station-b", "browser-2");
        mvc.perform(get(BASE).header("Authorization", AUTH)).andExpect(jsonPath("$.data", hasSize(2)));
        mvc.perform(get(BASE).param("bindingClientId", "station-a").header("Authorization", AUTH))
            .andExpect(jsonPath("$.data", hasSize(1))).andExpect(jsonPath("$.data[0].code").value("CAM-1"));
    }

    @Test void validatesRequiredAndLength() throws Exception {
        mvc.perform(post(BASE).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"code\":\"C\",\"purpose\":\"P\",\"bindingClientId\":\"S\",\"browserDeviceId\":\"D\"}"))
            .andExpect(jsonPath("$.code").value(500));
        String longClient = "x".repeat(129);
        mvc.perform(post(BASE).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(createJson("Camera", "CAM-1", longClient, "browser-1", "")))
            .andExpect(jsonPath("$.code").value(500));
    }

    @Test void rejectsDuplicateCode() throws Exception {
        create("One", "CAM-1", "station-a", "browser-1");
        mvc.perform(post(BASE).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(createJson("Two", "CAM-1", "station-b", "browser-2", "")))
            .andExpect(jsonPath("$.code").value(500)).andExpect(jsonPath("$.message").value("设备编码已存在"));
    }

    @Test void rejectsDuplicateBinding() throws Exception {
        create("One", "CAM-1", "station-a", "browser-1");
        mvc.perform(post(BASE).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(createJson("Two", "CAM-2", "station-a", "browser-1", "")))
            .andExpect(jsonPath("$.code").value(500)).andExpect(jsonPath("$.message").value("该浏览器摄像头绑定已存在"));
    }

    @Test void updateClearsOptionalFieldsAndCannotChangeBinding() throws Exception {
        long id = create("One", "CAM-1", "station-a", "browser-1");
        String body = "{\"name\":\"Updated\",\"code\":\"CAM-1\",\"purpose\":\"monitor\",\"location\":\"\",\"remark\":\"\",\"enabled\":false,\"bindingClientId\":\"hacked\",\"browserDeviceId\":\"hacked\"}";
        mvc.perform(put(BASE + "/" + id).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(jsonPath("$.data.location").value("")).andExpect(jsonPath("$.data.remark").value(""))
            .andExpect(jsonPath("$.data.status").value("DISABLED")).andExpect(jsonPath("$.data.bindingClientId").value("station-a"))
            .andExpect(jsonPath("$.data.browserDeviceId").value("browser-1"));
    }

    @Test void explicitRebindReplacesAllBindingFields() throws Exception {
        long id = create("One", "CAM-1", "station-a", "browser-1");
        mvc.perform(post(BASE + "/" + id + "/rebind").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bindingClientId\":\"station-b\",\"browserDeviceId\":\"browser-2\",\"browserGroupId\":\"group-2\",\"deviceLabel\":\"USB Camera\"}"))
            .andExpect(jsonPath("$.data.bindingClientId").value("station-b")).andExpect(jsonPath("$.data.browserDeviceId").value("browser-2"))
            .andExpect(jsonPath("$.data.browserGroupId").value("group-2")).andExpect(jsonPath("$.data.deviceLabel").value("USB Camera"))
            .andExpect(jsonPath("$.data.browserDeviceHash").doesNotExist());
        String hash = devices.findById(id).orElseThrow().browserDeviceHash;
        org.junit.jupiter.api.Assertions.assertEquals(64, hash.length());
        org.junit.jupiter.api.Assertions.assertEquals("3310469cdfeaf7b3d828ed582aed719b4edd951367c96bd48df930a2aa3d16d0", hash);
    }

    @Test void toggleUsesOnlyEnabledAndDisabled() throws Exception {
        long id = create("One", "CAM-1", "station-a", "browser-1");
        mvc.perform(post(BASE + "/" + id + "/toggle").header("Authorization", AUTH)).andExpect(jsonPath("$.data.status").value("DISABLED"));
        mvc.perform(post(BASE + "/" + id + "/toggle").header("Authorization", AUTH)).andExpect(jsonPath("$.data.status").value("ENABLED"));
    }

    @Test void deleteAndMissingDevice() throws Exception {
        long id = create("One", "CAM-1", "station-a", "browser-1");
        mvc.perform(delete(BASE + "/" + id).header("Authorization", AUTH)).andExpect(jsonPath("$.data").value(true));
        mvc.perform(get(BASE + "/" + id).header("Authorization", AUTH)).andExpect(jsonPath("$.code").value(500));
    }

    @Test void legacyWriteEndpointsReturn405() throws Exception {
        mvc.perform(post("/api/video-devices").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/video-devices/1").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/video-devices/1").header("Authorization", AUTH)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/video-devices/1/toggle").header("Authorization", AUTH)).andExpect(status().isMethodNotAllowed());
    }

    @Test void specializedApiExcludesNonLocalDevices() throws Exception {
        VideoDevice legacy = new VideoDevice();
        legacy.name = "GB camera";
        legacy.code = "GB-1";
        legacy.deviceType = "GB28181";
        legacy.status = "ENABLED";
        long id = devices.save(legacy).id;
        mvc.perform(get(BASE).header("Authorization", AUTH)).andExpect(jsonPath("$.data", hasSize(0)));
        mvc.perform(get(BASE + "/" + id).header("Authorization", AUTH)).andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("本地摄像头不存在"));
    }

    private long create(String name, String code, String client, String browser) throws Exception {
        String response = mvc.perform(post(BASE).header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(createJson(name, code, client, browser, "")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).path("data").path("id").asLong();
    }

    private String createJson(String name, String code, String client, String browser, String extra) {
        return "{\"name\":\"" + name + "\",\"code\":\"" + code + "\",\"purpose\":\"inspection\",\"location\":\"workshop\",\"remark\":\"demo\",\"enabled\":true,\"bindingClientId\":\"" + client + "\",\"browserDeviceId\":\"" + browser + "\",\"browserGroupId\":\"group-1\",\"deviceLabel\":\"Camera\"" + extra + "}";
    }
}
