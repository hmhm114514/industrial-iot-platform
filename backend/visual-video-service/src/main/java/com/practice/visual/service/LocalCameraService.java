package com.practice.visual.service;

import com.practice.visual.dao.VideoDeviceDao;
import com.practice.visual.dto.LocalCameraDtos.CreateRequest;
import com.practice.visual.dto.LocalCameraDtos.RebindRequest;
import com.practice.visual.dto.LocalCameraDtos.UpdateRequest;
import com.practice.visual.entity.VideoDevice;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalCameraService {
    public static final String LOCAL_CAMERA = "LOCAL_CAMERA";
    private final VideoDeviceDao dao;

    public LocalCameraService(VideoDeviceDao dao) { this.dao = dao; }

    public List<VideoDevice> list(String bindingClientId) {
        return bindingClientId == null
            ? dao.findAllByDeviceTypeOrderByIdAsc(LOCAL_CAMERA)
            : dao.findAllByDeviceTypeAndBindingClientIdOrderByIdAsc(LOCAL_CAMERA, bindingClientId);
    }

    public VideoDevice get(Long id) {
        VideoDevice device = dao.findById(id).orElseThrow(() -> new NoSuchElementException("本地摄像头不存在"));
        if (!LOCAL_CAMERA.equals(device.deviceType)) throw new NoSuchElementException("本地摄像头不存在");
        return device;
    }

    @Transactional
    public VideoDevice create(CreateRequest request) {
        String deviceHash = hashBrowserDeviceId(request.browserDeviceId);
        checkCode(request.code, null);
        checkBinding(request.bindingClientId, deviceHash, null);
        VideoDevice device = new VideoDevice();
        device.deviceType = LOCAL_CAMERA;
        device.name = request.name;
        device.code = request.code;
        device.purpose = request.purpose;
        device.location = request.location;
        device.remark = request.remark;
        device.status = Boolean.FALSE.equals(request.enabled) ? "DISABLED" : "ENABLED";
        device.bindingClientId = request.bindingClientId;
        device.browserDeviceId = request.browserDeviceId;
        device.browserDeviceHash = deviceHash;
        device.browserGroupId = request.browserGroupId;
        device.deviceLabel = request.deviceLabel;
        return dao.save(device);
    }

    @Transactional
    public VideoDevice update(Long id, UpdateRequest request) {
        VideoDevice device = get(id);
        checkCode(request.code, id);
        device.name = request.name;
        device.code = request.code;
        device.purpose = request.purpose;
        device.location = request.location;
        device.remark = request.remark;
        if (request.enabled != null) device.status = request.enabled ? "ENABLED" : "DISABLED";
        return dao.save(device);
    }

    @Transactional
    public VideoDevice rebind(Long id, RebindRequest request) {
        VideoDevice device = get(id);
        String deviceHash = hashBrowserDeviceId(request.browserDeviceId);
        checkBinding(request.bindingClientId, deviceHash, id);
        device.bindingClientId = request.bindingClientId;
        device.browserDeviceId = request.browserDeviceId;
        device.browserDeviceHash = deviceHash;
        device.browserGroupId = request.browserGroupId;
        device.deviceLabel = request.deviceLabel;
        return dao.save(device);
    }

    @Transactional
    public VideoDevice toggle(Long id) {
        VideoDevice device = get(id);
        device.status = "ENABLED".equals(device.status) ? "DISABLED" : "ENABLED";
        return dao.save(device);
    }

    @Transactional
    public boolean delete(Long id) {
        dao.delete(get(id));
        return true;
    }

    private void checkCode(String code, Long excludedId) {
        boolean duplicate = excludedId == null ? dao.existsByCode(code) : dao.existsByCodeAndIdNot(code, excludedId);
        if (duplicate) throw new IllegalArgumentException("设备编码已存在");
    }

    private void checkBinding(String clientId, String deviceHash, Long excludedId) {
        boolean duplicate = excludedId == null
            ? dao.existsByBindingClientIdAndBrowserDeviceHash(clientId, deviceHash)
            : dao.existsByBindingClientIdAndBrowserDeviceHashAndIdNot(clientId, deviceHash, excludedId);
        if (duplicate) throw new IllegalArgumentException("该浏览器摄像头绑定已存在");
    }

    private String hashBrowserDeviceId(String browserDeviceId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(browserDeviceId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
