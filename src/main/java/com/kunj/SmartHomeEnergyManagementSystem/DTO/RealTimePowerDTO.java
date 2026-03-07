package com.kunj.SmartHomeEnergyManagementSystem.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealTimePowerDTO
{
    private Double totalPower; // Current total power in Watts
    private Double totalCurrent; // Total current draw in Amperes
    private Double averageVoltage; // Average voltage across devices
    private Integer activeDevices; // Number of online devices
    private Integer totalDevices; // Total devices
    private LocalDateTime timestamp;
    private String status; // "NORMAL", "HIGH_USAGE", "CRITICAL"
    private List<DevicePowerDTO> deviceBreakdown;

    // Getter & Setter

    public Double getTotalPower() {
        return totalPower;
    }

    public void setTotalPower(Double totalPower) {
        this.totalPower = totalPower;
    }

    public Double getTotalCurrent() {
        return totalCurrent;
    }

    public void setTotalCurrent(Double totalCurrent) {
        this.totalCurrent = totalCurrent;
    }

    public Double getAverageVoltage() {
        return averageVoltage;
    }

    public void setAverageVoltage(Double averageVoltage) {
        this.averageVoltage = averageVoltage;
    }

    public Integer getActiveDevices() {
        return activeDevices;
    }

    public void setActiveDevices(Integer activeDevices) {
        this.activeDevices = activeDevices;
    }

    public Integer getTotalDevices() {
        return totalDevices;
    }

    public void setTotalDevices(Integer totalDevices) {
        this.totalDevices = totalDevices;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<DevicePowerDTO> getDeviceBreakdown() {
        return deviceBreakdown;
    }

    public void setDeviceBreakdown(List<DevicePowerDTO> deviceBreakdown) {
        this.deviceBreakdown = deviceBreakdown;
    }
}
