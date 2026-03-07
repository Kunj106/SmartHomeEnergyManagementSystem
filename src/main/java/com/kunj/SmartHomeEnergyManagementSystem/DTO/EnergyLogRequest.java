package com.kunj.SmartHomeEnergyManagementSystem.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EnergyLogRequest
{
    @NotNull(message = "Device ID is required")
    private Long deviceId;

    @NotNull(message = "Energy consumed is required")
    private Double energyConsumed;

    @NotNull(message = "Power in watts is required")
    private Double powerWatts;

    private LocalDateTime timestamp;
    private Integer durationMinutes;
    private Double costEstimate;
    private String logType;

    // Getter & Setter

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Double getEnergyConsumed() {
        return energyConsumed;
    }

    public void setEnergyConsumed(Double energyConsumed) {
        this.energyConsumed = energyConsumed;
    }

    public Double getPowerWatts() {
        return powerWatts;
    }

    public void setPowerWatts(Double powerWatts) {
        this.powerWatts = powerWatts;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Double getCostEstimate() {
        return costEstimate;
    }

    public void setCostEstimate(Double costEstimate) {
        this.costEstimate = costEstimate;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }
}
