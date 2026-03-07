package com.kunj.SmartHomeEnergyManagementSystem.Controller;

import com.kunj.SmartHomeEnergyManagementSystem.DTO.DeviceRequest;
import com.kunj.SmartHomeEnergyManagementSystem.DTO.DeviceResponse;
import com.kunj.SmartHomeEnergyManagementSystem.Service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DeviceController
{
    @Autowired
    private DeviceService deviceService;

    // Create new device
    @PostMapping
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> createDevice(@Valid @RequestBody DeviceRequest request) {
        try {
            DeviceResponse device = deviceService.createDevice(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(device);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get all user devices
    @GetMapping
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getAllDevices() {
        try {
            List<DeviceResponse> devices = deviceService.getAllUserDevices();
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get device by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getDeviceById(@PathVariable Long id) {
        try {
            DeviceResponse device = deviceService.getDeviceById(id);
            return ResponseEntity.ok(device);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // Update device
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody DeviceRequest request) {
        try {
            DeviceResponse device = deviceService.updateDevice(id, request);
            return ResponseEntity.ok(device);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Delete device
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> deleteDevice(@PathVariable Long id) {
        try {
            deviceService.deleteDevice(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Device deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get devices by type
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getDevicesByType(@PathVariable String type) {
        try {
            List<DeviceResponse> devices = deviceService.getDevicesByType(type);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get devices by status
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getDevicesByStatus(@PathVariable String status) {
        try {
            List<DeviceResponse> devices = deviceService.getDevicesByStatus(status);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get active devices
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getActiveDevices() {
        try {
            List<DeviceResponse> devices = deviceService.getActiveDevices();
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Update device status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> updateDeviceStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            DeviceResponse device = deviceService.updateDeviceStatus(id, status);
            return ResponseEntity.ok(device);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get device statistics
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getDeviceStatistics() {
        try {
            DeviceService.DeviceStatistics stats = deviceService.getDeviceStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Search devices
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> searchDevices(@RequestParam String query) {
        try {
            List<DeviceResponse> devices = deviceService.searchDevices(query);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Helper method to create error response
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
