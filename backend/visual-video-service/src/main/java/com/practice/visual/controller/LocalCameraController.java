package com.practice.visual.controller;

import com.practice.common.ApiResponse;
import com.practice.visual.dto.LocalCameraDtos.CreateRequest;
import com.practice.visual.dto.LocalCameraDtos.RebindRequest;
import com.practice.visual.dto.LocalCameraDtos.UpdateRequest;
import com.practice.visual.entity.VideoDevice;
import com.practice.visual.service.LocalCameraService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/video-devices/local-cameras")
public class LocalCameraController {
    private final LocalCameraService service;

    public LocalCameraController(LocalCameraService service) { this.service = service; }

    @GetMapping public ApiResponse<List<VideoDevice>> list(@RequestParam(required = false) String bindingClientId) { return ApiResponse.ok(service.list(bindingClientId)); }
    @GetMapping("/{id}") public ApiResponse<VideoDevice> get(@PathVariable Long id) { return ApiResponse.ok(service.get(id)); }
    @PostMapping public ApiResponse<VideoDevice> create(@Valid @RequestBody CreateRequest request) { return ApiResponse.ok(service.create(request)); }
    @PutMapping("/{id}") public ApiResponse<VideoDevice> update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request) { return ApiResponse.ok(service.update(id, request)); }
    @PostMapping("/{id}/rebind") public ApiResponse<VideoDevice> rebind(@PathVariable Long id, @Valid @RequestBody RebindRequest request) { return ApiResponse.ok(service.rebind(id, request)); }
    @PostMapping("/{id}/toggle") public ApiResponse<VideoDevice> toggle(@PathVariable Long id) { return ApiResponse.ok(service.toggle(id)); }
    @DeleteMapping("/{id}") public ApiResponse<Boolean> delete(@PathVariable Long id) { return ApiResponse.ok(service.delete(id)); }
}
