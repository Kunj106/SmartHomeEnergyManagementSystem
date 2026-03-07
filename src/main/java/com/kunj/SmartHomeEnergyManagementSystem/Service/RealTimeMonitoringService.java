package com.kunj.SmartHomeEnergyManagementSystem.Service;

import com.kunj.SmartHomeEnergyManagementSystem.DTO.*;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.Device;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.IoTReading;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.ReadingStatus;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.DeviceRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.IoTReadingRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RealTimeMonitoringService
{
    private final IoTReadingRepository iotReadingRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;


     // Get real-time power consumption for user's active devices
    public RealTimePowerDTO getRealTimePower(Long userId) {
        log.info("Fetching real-time power for user: {}", userId);

        // Get latest readings for user's active devices
        LocalDateTime since = LocalDateTime.now().minusMinutes(2); // Last 2 minutes
        List<IoTReading> latestReadings = iotReadingRepository
                .findLatestActiveDeviceReadings(userId, since);

        if (latestReadings.isEmpty()) {
            return RealTimePowerDTO.builder()
                    .totalPower(0.0)
                    .totalCurrent(0.0)
                    .averageVoltage(0.0)
                    .activeDevices(0)
                    .totalDevices(deviceRepository.countByUserId(userId).intValue())
                    .timestamp(LocalDateTime.now())
                    .status("NO_DATA")
                    .deviceBreakdown(new ArrayList<>())
                    .build();
        }

        // Calculate aggregates
        double totalPower = latestReadings.stream()
                .mapToDouble(IoTReading::getPower)
                .sum();

        double totalCurrent = latestReadings.stream()
                .mapToDouble(IoTReading::getCurrent)
                .sum();

        double averageVoltage = latestReadings.stream()
                .mapToDouble(IoTReading::getVoltage)
                .average()
                .orElse(0.0);

        // Build device breakdown
        List<DevicePowerDTO> deviceBreakdown = latestReadings.stream()
                .map(this::toDevicePowerDTO)
                .collect(Collectors.toList());

        // Determine overall status
        String status = determineSystemStatus(totalPower, latestReadings);

        return RealTimePowerDTO.builder()
                .totalPower(Math.round(totalPower * 100.0) / 100.0)
                .totalCurrent(Math.round(totalCurrent * 100.0) / 100.0)
                .averageVoltage(Math.round(averageVoltage * 10.0) / 10.0)
                .activeDevices(latestReadings.size())
                .totalDevices(deviceRepository.countByUserId(userId).intValue())
                .timestamp(LocalDateTime.now())
                .status(status)
                .deviceBreakdown(deviceBreakdown)
                .build();
    }

     // Get power consumption trend over time
    public List<PowerTrendDTO> getPowerTrend(Long userId, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);

        List<IoTReading> readings = iotReadingRepository
                .findLatestActiveDeviceReadings(userId, since);

        // Group by minute and aggregate
        Map<LocalDateTime, List<IoTReading>> groupedByMinute = readings.stream()
                .collect(Collectors.groupingBy(r ->
                        r.getTimestamp().withSecond(0).withNano(0)));

        return groupedByMinute.entrySet().stream()
                .map(entry -> {
                    double totalPower = entry.getValue().stream()
                            .mapToDouble(IoTReading::getPower)
                            .sum();

                    long uniqueDevices = entry.getValue().stream()
                            .map(r -> r.getDevice().getId())
                            .distinct()
                            .count();

                    return PowerTrendDTO.builder()
                            .timestamp(entry.getKey())
                            .totalPower(Math.round(totalPower * 100.0) / 100.0)
                            .activeDevices((int) uniqueDevices)
                            .build();
                })
                .sorted(Comparator.comparing(PowerTrendDTO::getTimestamp))
                .collect(Collectors.toList());
    }

     // Get real-time chart data for dashboard
    public RealTimeChartDataDTO getRealTimeChartData(Long userId, int dataPoints) {
        List<PowerTrendDTO> trends = getPowerTrend(userId, dataPoints);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        List<String> labels = trends.stream()
                .map(t -> t.getTimestamp().format(formatter))
                .collect(Collectors.toList());

        List<Double> powerData = trends.stream()
                .map(PowerTrendDTO::getTotalPower)
                .collect(Collectors.toList());

        List<Integer> activeDeviceData = trends.stream()
                .map(PowerTrendDTO::getActiveDevices)
                .collect(Collectors.toList());

        return RealTimeChartDataDTO.builder()
                .labels(labels)
                .powerData(powerData)
                .activeDeviceData(activeDeviceData)
                .build();
    }

     // Get power consumption by device type
    public List<DeviceTypeConsumptionDTO> getConsumptionByDeviceType(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(2);
        List<IoTReading> readings = iotReadingRepository
                .findLatestActiveDeviceReadings(userId, since);

        // Group by device type
        Map<String, List<IoTReading>> byType = readings.stream()
                .collect(Collectors.groupingBy(r -> r.getDevice().getType()));

        double totalPower = readings.stream()
                .mapToDouble(IoTReading::getPower)
                .sum();

        return byType.entrySet().stream()
                .map(entry -> {
                    double typePower = entry.getValue().stream()
                            .mapToDouble(IoTReading::getPower)
                            .sum();

                    int deviceCount = (int) entry.getValue().stream()
                            .map(r -> r.getDevice().getId())
                            .distinct()
                            .count();

                    double percentage = totalPower > 0 ? (typePower / totalPower * 100.0) : 0.0;

                    return DeviceTypeConsumptionDTO.builder()
                            .deviceType(entry.getKey())
                            .totalPower(Math.round(typePower * 100.0) / 100.0)
                            .deviceCount(deviceCount)
                            .percentage(Math.round(percentage * 10.0) / 10.0)
                            .build();
                })
                .sorted(Comparator.comparing(DeviceTypeConsumptionDTO::getTotalPower).reversed())
                .collect(Collectors.toList());
    }


     // Get latest IoT readings for a specific device
    public List<IoTReadingDTO> getDeviceReadings(Long deviceId, int limit) {
        LocalDateTime since = LocalDateTime.now().minusHours(1);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        List<IoTReading> readings = iotReadingRepository
                .findRecentByDeviceId(deviceId, since);

        return readings.stream()
                .limit(limit)
                .map(this::toIoTReadingDTO)
                .collect(Collectors.toList());
    }

    // Get device health status
    public DeviceHealthDTO getDeviceHealth(Long deviceId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Get statistics
        Double avgTemp = iotReadingRepository.getAverageTemperature(deviceId, since);
        Long anomalyCount = iotReadingRepository.countAnomaliesByDeviceId(deviceId, since);

        // Get recent readings for trend analysis
        List<IoTReading> recentReadings = iotReadingRepository
                .findRecentByDeviceId(deviceId, LocalDateTime.now().minusHours(2));

        double temperatureTrend = calculateTemperatureTrend(recentReadings);
        String healthStatus = determineHealthStatus(avgTemp, anomalyCount, temperatureTrend);
        List<String> warnings = generateWarnings(device, avgTemp, anomalyCount, temperatureTrend);

        return DeviceHealthDTO.builder()
                .deviceId(deviceId)
                .deviceName(device.getName())
                .healthStatus(healthStatus)
                .averageTemperature(avgTemp != null ? Math.round(avgTemp * 10.0) / 10.0 : 0.0)
                .temperatureTrend(Math.round(temperatureTrend * 10.0) / 10.0)
                .anomalyCount(anomalyCount.intValue())
                .lastHealthCheck(LocalDateTime.now())
                .warnings(warnings)
                .build();
    }

     // Get power alerts for user
    public List<PowerAlertDTO> getPowerAlerts(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        List<IoTReading> criticalReadings = iotReadingRepository
                .findCriticalReadings(userId, since);

        return criticalReadings.stream()
                .map(reading -> PowerAlertDTO.builder()
                        .id(reading.getId())
                        .alertType(reading.getStatus().name())
                        .severity(determineSeverity(reading))
                        .message(generateAlertMessage(reading))
                        .deviceId(reading.getDevice().getId())
                        .deviceName(reading.getDevice().getName())
                        .timestamp(reading.getTimestamp())
                        .acknowledged(false)
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    private DevicePowerDTO toDevicePowerDTO(IoTReading reading) {
        Device device = reading.getDevice();

        return DevicePowerDTO.builder()
                .deviceId(device.getId())
                .deviceName(device.getName())
                .deviceType(device.getType())
                .currentPower(Math.round(reading.getPower() * 100.0) / 100.0)
                .voltage(Math.round(reading.getVoltage() * 10.0) / 10.0)
                .current(Math.round(reading.getCurrent() * 100.0) / 100.0)
                .temperature(Math.round(reading.getTemperature() * 10.0) / 10.0)
                .status(reading.getStatus().name())
                .lastReading(reading.getTimestamp())
                .build();
    }

    private IoTReadingDTO toIoTReadingDTO(IoTReading reading) {
        return IoTReadingDTO.builder()
                .id(reading.getId())
                .deviceId(reading.getDevice().getId())
                .deviceName(reading.getDevice().getName())
                .voltage(reading.getVoltage())
                .current(reading.getCurrent())
                .power(reading.getPower())
                .energy(reading.getEnergy())
                .powerFactor(reading.getPowerFactor())
                .frequency(reading.getFrequency())
                .temperature(reading.getTemperature())
                .status(reading.getStatus().name())
                .timestamp(reading.getTimestamp())
                .isAnomaly(reading.getIsAnomaly())
                .build();
    }

    private String determineSystemStatus(double totalPower, List<IoTReading> readings) {
        // Check for critical readings
        long criticalCount = readings.stream()
                .filter(r -> r.getStatus() == ReadingStatus.CRITICAL)
                .count();

        if (criticalCount > 0) {
            return "CRITICAL";
        }

        // Check for high usage (>5kW)
        if (totalPower > 5000) {
            return "HIGH_USAGE";
        }

        // Check for warnings
        long warningCount = readings.stream()
                .filter(r -> r.getStatus() == ReadingStatus.WARNING)
                .count();

        if (warningCount > 0) {
            return "WARNING";
        }

        return "NORMAL";
    }

    private double calculateTemperatureTrend(List<IoTReading> readings) {
        if (readings.size() < 2) {
            return 0.0;
        }

        // Compare average of first half vs second half
        int midpoint = readings.size() / 2;

        double firstHalfAvg = readings.subList(0, midpoint).stream()
                .mapToDouble(IoTReading::getTemperature)
                .average()
                .orElse(0.0);

        double secondHalfAvg = readings.subList(midpoint, readings.size()).stream()
                .mapToDouble(IoTReading::getTemperature)
                .average()
                .orElse(0.0);

        return secondHalfAvg - firstHalfAvg;
    }

    private String determineHealthStatus(Double avgTemp, Long anomalyCount, double trend) {
        if (avgTemp == null) {
            return "NO_DATA";
        }

        if (avgTemp > 80 || anomalyCount > 10 || trend > 10) {
            return "FAULTY";
        }

        if (avgTemp > 60 || anomalyCount > 5 || trend > 5) {
            return "DEGRADED";
        }

        return "HEALTHY";
    }

    private List<String> generateWarnings(Device device, Double avgTemp,
                                          Long anomalyCount, double trend) {
        List<String> warnings = new ArrayList<>();

        if (avgTemp != null && avgTemp > 70) {
            warnings.add("High average temperature detected");
        }

        if (trend > 5) {
            warnings.add("Temperature increasing rapidly");
        }

        if (anomalyCount > 5) {
            warnings.add("Multiple anomalies detected in last 24 hours");
        }

        return warnings;
    }

    private String determineSeverity(IoTReading reading) {
        switch (reading.getStatus()) {
            case CRITICAL:
                return "CRITICAL";
            case WARNING:
                return "MEDIUM";
            case ERROR:
                return "HIGH";
            default:
                return "LOW";
        }
    }

    private String generateAlertMessage(IoTReading reading) {
        Device device = reading.getDevice();

        switch (reading.getStatus()) {
            case CRITICAL:
                return String.format("%s is consuming %.0fW (%.0f%% over rated capacity)",
                        device.getName(),
                        reading.getPower(),
                        (reading.getPower() / device.getPowerRating() - 1) * 100);

            case WARNING:
                return String.format("%s power consumption is elevated at %.0fW",
                        device.getName(), reading.getPower());

            default:
                return String.format("Unusual reading detected for %s", device.getName());
        }
    }
}
