package com.kunj.SmartHomeEnergyManagementSystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_readings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IoTReading
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "voltage", nullable = false)
    private Double voltage; // Volts

    @Column(name = "current", nullable = false)
    private Double current; // Amperes

    @Column(name = "power", nullable = false)
    private Double power; // Watts (real-time power = voltage * current)

    @Column(name = "energy", nullable = false)
    private Double energy; // kWh (cumulative since device turned on)

    @Column(name = "power_factor")
    private Double powerFactor; // 0.0 to 1.0

    @Column(name = "frequency")
    private Double frequency; // Hz (typically 50 or 60)

    @Column(name = "temperature")
    private Double temperature; // Celsius (device temperature)

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReadingStatus status; // NORMAL, WARNING, CRITICAL

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_anomaly")
    private Boolean isAnomaly; // Flag for unusual readings

    @Column(name = "data_source")
    private String dataSource; // "SIMULATED" or "API_PROVIDER"

    // Getter & Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Double getVoltage() {
        return voltage;
    }

    public void setVoltage(Double voltage) {
        this.voltage = voltage;
    }

    public Double getCurrent() {
        return current;
    }

    public void setCurrent(Double current) {
        this.current = current;
    }

    public Double getPower() {
        return power;
    }

    public void setPower(Double power) {
        this.power = power;
    }

    public Double getEnergy() {
        return energy;
    }

    public void setEnergy(Double energy) {
        this.energy = energy;
    }

    public Double getPowerFactor() {
        return powerFactor;
    }

    public void setPowerFactor(Double powerFactor) {
        this.powerFactor = powerFactor;
    }

    public Double getFrequency() {
        return frequency;
    }

    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getAnomaly() {
        return isAnomaly;
    }

    public void setAnomaly(Boolean anomaly) {
        isAnomaly = anomaly;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
}
