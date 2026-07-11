package com.practice.visual.dao;

import com.practice.visual.entity.VideoDevice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoDeviceDao extends JpaRepository<VideoDevice, Long> {
    List<VideoDevice> findAllByDeviceTypeOrderByIdAsc(String deviceType);
    List<VideoDevice> findAllByDeviceTypeAndBindingClientIdOrderByIdAsc(String deviceType, String bindingClientId);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    boolean existsByBindingClientIdAndBrowserDeviceHash(String bindingClientId, String browserDeviceHash);
    boolean existsByBindingClientIdAndBrowserDeviceHashAndIdNot(String bindingClientId, String browserDeviceHash, Long id);
}
