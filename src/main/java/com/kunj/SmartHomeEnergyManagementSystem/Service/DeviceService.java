package com.kunj.SmartHomeEnergyManagementSystem.Service;

import com.kunj.SmartHomeEnergyManagementSystem.DTO.DeviceRequest;
import com.kunj.SmartHomeEnergyManagementSystem.DTO.DeviceResponse;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.Device;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.User;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.DeviceRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceService
{
    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    // Get current authenticated user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Create new device
    @Transactional
    public DeviceResponse createDevice(DeviceRequest request) {
        User currentUser = getCurrentUser();

        Device device = new Device();
        device.setName(request.getName());
        device.setType(request.getType());
        device.setPowerRating(request.getPowerRating());
        device.setVoltageRating(request.getVoltageRating());
        device.setInstallationDate(request.getInstallationDate() != null ? request.getInstallationDate() : LocalDateTime.now());
        device.setLocation(request.getLocation());
        device.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        device.setStatus(request.getStatus() != null ? request.getStatus() : "offline");
        device.setOwner(currentUser);

        Device savedDevice = deviceRepository.save(device);
        return mapToResponse(savedDevice);
    }

    // Get all devices for current user
    public List<DeviceResponse> getAllUserDevices() {
        User currentUser = getCurrentUser();
        List<Device> devices = deviceRepository.findByOwner(currentUser);
        return devices.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Get device by ID (with ownership check)
    public DeviceResponse getDeviceById(Long deviceId) {
        User currentUser = getCurrentUser();
        Device device = deviceRepository.findByIdAndOwner(deviceId, currentUser)
                .orElseThrow(() -> new RuntimeException("Device not found or you don't have permission"));
        return mapToResponse(device);
    }

    // Update device (with ownership check)
    @Transactional
    public DeviceResponse updateDevice(Long deviceId, DeviceRequest request) {
        User currentUser = getCurrentUser();
        Device device = deviceRepository.findByIdAndOwner(deviceId, currentUser)
                .orElseThrow(() -> new RuntimeException("Device not found or you don't have permission"));

        device.setName(request.getName());
        device.setType(request.getType());
        device.setPowerRating(request.getPowerRating());
        device.setVoltageRating(request.getVoltageRating());
        device.setInstallationDate(request.getInstallationDate());
        device.setLocation(request.getLocation());
        if (request.getIsActive() != null) {
            device.setIsActive(request.getIsActive());
        }
        if (request.getStatus() != null) {
            device.setStatus(request.getStatus());
        }

        Device updatedDevice = deviceRepository.save(device);
        return mapToResponse(updatedDevice);
    }

    // Delete device (with ownership check)
    @Transactional
    public void deleteDevice(Long deviceId) {
        User currentUser = getCurrentUser();
        Device device = deviceRepository.findByIdAndOwner(deviceId, currentUser)
                .orElseThrow(() -> new RuntimeException("Device not found or you don't have permission"));
        deviceRepository.delete(device);
    }

    // Get devices by type
    public List<DeviceResponse> getDevicesByType(String type) {
        User currentUser = getCurrentUser();
        List<Device> devices = deviceRepository.findByTypeAndOwner(type, currentUser);
        return devices.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Get devices by status
    public List<DeviceResponse> getDevicesByStatus(String status) {
        User currentUser = getCurrentUser();
        List<Device> devices = deviceRepository.findByStatusAndOwner(status, currentUser);
        return devices.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Get active devices
    public List<DeviceResponse> getActiveDevices() {
        User currentUser = getCurrentUser();
        List<Device> devices = deviceRepository.findByOwnerAndIsActive(currentUser, true);
        return devices.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Update device status
    @Transactional
    public DeviceResponse updateDeviceStatus(Long deviceId, String status) {
        User currentUser = getCurrentUser();
        Device device = deviceRepository.findByIdAndOwner(deviceId, currentUser)
                .orElseThrow(() -> new RuntimeException("Device not found or you don't have permission"));

        device.setStatus(status);
        Device updatedDevice = deviceRepository.save(device);
        return mapToResponse(updatedDevice);
    }

    // Get device statistics
    public DeviceStatistics getDeviceStatistics() {
        User currentUser = getCurrentUser();

        Long totalDevices = deviceRepository.countByOwner(currentUser);
        Long activeDevices = deviceRepository.countByOwnerAndIsActive(currentUser, true);
        Double totalPowerRating = deviceRepository.getTotalPowerRatingByOwner(currentUser);

        return new DeviceStatistics(
                totalDevices,
                activeDevices,
                totalPowerRating != null ? totalPowerRating : 0.0
        );
    }

    // Search devices by name
    public List<DeviceResponse> searchDevices(String query) {
        User currentUser = getCurrentUser();
        List<Device> devices = deviceRepository.findByOwnerAndNameContainingIgnoreCase(currentUser, query);
        return devices.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Helper method to map Device to DeviceResponse
    private DeviceResponse mapToResponse(Device device) {
        DeviceResponse response = new DeviceResponse();
        response.setId(device.getId());
        response.setName(device.getName());
        response.setType(device.getType());
        response.setPowerRating(device.getPowerRating());
        response.setVoltageRating(device.getVoltageRating());
        response.setInstallationDate(device.getInstallationDate());
        response.setLocation(device.getLocation());
        response.setIsActive(device.getIsActive());
        response.setStatus(device.getStatus());
        response.setOwnerId(device.getOwner().getId());
        response.setOwnerUsername(device.getOwner().getUsername());
        response.setCreatedAt(device.getCreatedAt());
        response.setUpdatedAt(device.getUpdatedAt());
        return response;
    }

    // Inner class for statistics
    @Data
    @AllArgsConstructor
    public static class DeviceStatistics {
        private Long totalDevices;
        private Long activeDevices;
        private Double totalPowerRating;
    }
}
