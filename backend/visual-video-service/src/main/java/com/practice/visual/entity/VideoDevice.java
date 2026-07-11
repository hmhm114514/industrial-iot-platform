package com.practice.visual.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.practice.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_video_device_code", columnNames = "code"),
    @UniqueConstraint(name = "uk_video_device_binding_hash", columnNames = {"binding_client_id", "browser_device_hash"})
})
public class VideoDevice extends BaseEntity {
    @Column(length = 30)
    public String deviceType = "LOCAL_CAMERA";

    @Column(length = 128)
    public String bindingClientId;

    @Column(length = 1024)
    public String browserDeviceId;

    @JsonIgnore
    @Column(length = 64)
    public String browserDeviceHash;

    @Column(length = 255)
    public String browserGroupId;

    @Column(length = 255)
    public String deviceLabel;

    @Column(length = 255)
    public String purpose;

    @Column(length = 100)
    public String channelNo;

    @Column(length = 500)
    public String streamUrl;

    @Column(length = 255)
    public String location;
}
