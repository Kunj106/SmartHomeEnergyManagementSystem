package com.kunj.SmartHomeEnergyManagementSystem.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealTimeChartDataDTO
{
    private List<String> labels; // Time labels
    private List<Double> powerData; // Power consumption data
    private List<Integer> activeDeviceData; // Active devices count

    // Getter & Setter

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<Double> getPowerData() {
        return powerData;
    }

    public void setPowerData(List<Double> powerData) {
        this.powerData = powerData;
    }

    public List<Integer> getActiveDeviceData() {
        return activeDeviceData;
    }

    public void setActiveDeviceData(List<Integer> activeDeviceData) {
        this.activeDeviceData = activeDeviceData;
    }
}
