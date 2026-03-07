package com.kunj.SmartHomeEnergyManagementSystem.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceRequest
{
    @NotBlank(message = "Device name is required")
    private String name;

    @NotBlank(message = "Device type is required")
    private String type;

    @NotNull(message = "Power rating is required")
    private Double powerRating;

    private Double voltageRating;
    private Integer energyStarRating;
    private LocalDateTime installationDate;
    private String location;
    private Boolean isActive;
    private String status;

    // Getter & Setter

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getPowerRating() {
        return powerRating;
    }

    public void setPowerRating(Double powerRating) {
        this.powerRating = powerRating;
    }

    public Double getVoltageRating() {
        return voltageRating;
    }

    public void setVoltageRating(Double voltageRating) {
        this.voltageRating = voltageRating;
    }

    public Integer getEnergyStarRating() {
        return energyStarRating;
    }

    public void setEnergyStarRating(Integer energyStarRating) {
        this.energyStarRating = energyStarRating;
    }

    public LocalDateTime getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(LocalDateTime installationDate) {
        this.installationDate = installationDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
