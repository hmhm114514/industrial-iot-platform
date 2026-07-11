package com.practice.visual.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class LocalCameraDtos {
    private LocalCameraDtos() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateRequest {
        @NotBlank @Size(max = 100) public String name;
        @NotBlank @Size(max = 100) public String code;
        @NotBlank @Size(max = 255) public String purpose;
        @Size(max = 255) public String location;
        @Size(max = 255) public String remark;
        public Boolean enabled;
        @NotBlank @Size(max = 128) public String bindingClientId;
        @NotBlank @Size(max = 1024) public String browserDeviceId;
        @Size(max = 255) public String browserGroupId;
        @Size(max = 255) public String deviceLabel;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateRequest {
        @NotBlank @Size(max = 100) public String name;
        @NotBlank @Size(max = 100) public String code;
        @NotBlank @Size(max = 255) public String purpose;
        @Size(max = 255) public String location;
        @Size(max = 255) public String remark;
        public Boolean enabled;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RebindRequest {
        @NotBlank @Size(max = 128) public String bindingClientId;
        @NotBlank @Size(max = 1024) public String browserDeviceId;
        @Size(max = 255) public String browserGroupId;
        @Size(max = 255) public String deviceLabel;
    }
}
