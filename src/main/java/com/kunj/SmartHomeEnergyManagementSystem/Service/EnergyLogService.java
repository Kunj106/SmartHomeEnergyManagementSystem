package com.kunj.SmartHomeEnergyManagementSystem.Service;

import com.kunj.SmartHomeEnergyManagementSystem.DTO.EnergyLogRequest;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.Device;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.EnergyLog;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.User;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.DeviceRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.EnergyLogRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EnergyLogService
{
    @Autowired
    private EnergyLogRepository energyLogRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    private static final double COST_PER_KWH = 6; // ₹6 per kWh (configurable)

    // Get current authenticated user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Create energy log
    @Transactional
    public EnergyLogResponse createEnergyLog(EnergyLogRequest request) {
        User currentUser = getCurrentUser();

        // Verify device ownership
        Device device = deviceRepository.findByIdAndOwner(request.getDeviceId(), currentUser)
                .orElseThrow(() -> new RuntimeException("Device not found or you don't have permission"));

        EnergyLog log = new EnergyLog();
        log.setDevice(device);
        log.setUser(currentUser);
        log.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
        log.setEnergyConsumed(request.getEnergyConsumed());
        log.setPowerWatts(request.getPowerWatts());
        log.setDurationMinutes(request.getDurationMinutes());

        // Calculate cost if not provided
        Double cost = request.getCostEstimate();
        if (cost == null) {
            cost = request.getEnergyConsumed() * COST_PER_KWH;
        }
        log.setCostEstimate(cost);

        log.setLogType(request.getLogType() != null ? request.getLogType() : "manual");

        EnergyLog savedLog = energyLogRepository.save(log);
        return mapToLogResponse(savedLog);
    }

    // Get hourly consumption data for graphs (6 AM - 12 PM today)
    public List<GraphDataPoint> getHourlyConsumption() {
        User currentUser = getCurrentUser();
        List<EnergyLog> logs = energyLogRepository.getTodayHourlyLogsByUser(currentUser);

        // Group by hour and sum energy for each hour
        Map<Integer, Double> hourlyMap = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getTimestamp().getHour(),
                        Collectors.summingDouble(EnergyLog::getEnergyConsumed)
                ));

        // Create data points for 6 AM to 12 PM with proper labels
        List<GraphDataPoint> dataPoints = new ArrayList<>();
        for (int hour = 6; hour <= 12; hour++) {
            String label = formatHourLabel(hour);
            Double value = hourlyMap.getOrDefault(hour, 0.0);
            LocalDateTime timestamp = LocalDateTime.now().withHour(hour).withMinute(0);

            dataPoints.add(new GraphDataPoint(label, value, timestamp));
        }

        return dataPoints;
    }

    // Helper method to format hour labels
    private String formatHourLabel(int hour) {
        if (hour == 0) return "12 AM";
        if (hour < 12) return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }

    // Get daily consumption data for graphs (last 30 days)
    public List<GraphDataPoint> getDailyConsumption() {
        User currentUser = getCurrentUser();
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<EnergyLog> logs = energyLogRepository.getDailyLogsForLast30Days(currentUser, startDate);

        return logs.stream()
                .map(log -> new GraphDataPoint(
                        log.getTimestamp().format(DateTimeFormatter.ofPattern("MMM dd")),
                        log.getEnergyConsumed(),
                        log.getTimestamp()
                ))
                .collect(Collectors.toList());
    }

    // Get consumption by device for current month
    public List<DeviceConsumption> getConsumptionByDevice() {
        User currentUser = getCurrentUser();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().plusDays(31).atStartOfDay();

        List<Object[]> results = energyLogRepository.getEnergyByDeviceType(currentUser, startOfMonth, endOfMonth);

        return results.stream()
                .map(result -> new DeviceConsumption(
                        (String) result[0],  // device type
                        (Double) result[1]    // total energy
                ))
                .collect(Collectors.toList());
    }

    // Get consumption summary for date range
    public ConsumptionSummary getConsumptionSummary(LocalDateTime start, LocalDateTime end) {
        User currentUser = getCurrentUser();

        List<EnergyLog> logs = energyLogRepository.findByUserAndTimestampBetween(currentUser, start, end);

        if (logs.isEmpty()) {
            return new ConsumptionSummary(0.0, 0.0, 0.0, 0.0, 0, start, end);
        }

        Double totalEnergy = logs.stream()
                .mapToDouble(EnergyLog::getEnergyConsumed)
                .sum();

        Double averageEnergy = totalEnergy / logs.size();

        Double peakEnergy = logs.stream()
                .mapToDouble(EnergyLog::getEnergyConsumed)
                .max()
                .orElse(0.0);

        Double estimatedCost = totalEnergy * COST_PER_KWH;

        Set<Long> uniqueDevices = logs.stream()
                .map(log -> log.getDevice().getId())
                .collect(Collectors.toSet());

        return new ConsumptionSummary(
                totalEnergy,
                averageEnergy,
                peakEnergy,
                estimatedCost,
                uniqueDevices.size(),
                start,
                end
        );
    }

    // Get logs for specific device
    public List<EnergyLogResponse> getDeviceLogs(Long deviceId, LocalDateTime start, LocalDateTime end) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findByIdAndOwner(deviceId, currentUser)
                .orElseThrow(() -> new RuntimeException("Device not found or you don't have permission"));

        List<EnergyLog> logs;
        if (start != null && end != null) {
            logs = energyLogRepository.findByDeviceAndTimestampBetween(device, start, end);
        } else {
            logs = energyLogRepository.findByDevice(device);
        }

        return logs.stream().map(this::mapToLogResponse).collect(Collectors.toList());
    }

    // Generate hourly logs for 6 AM to 12 PM (for testing)
    @Transactional
    public void generateHourlyLogs() {
        User currentUser = getCurrentUser();
        List<Device> activeDevices = deviceRepository.findByOwnerAndIsActive(currentUser, true);

        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();

        // Generate logs for 6 AM to 12 PM (7 hours: 6, 7, 8, 9, 10, 11, 12)
        for (int hour = 6; hour <= 12; hour++) {
            LocalDateTime timestamp = today.withHour(hour).withMinute(0).withSecond(0);

            for (Device device : activeDevices) {
                // Check if log already exists for this hour and device
                List<EnergyLog> existing = energyLogRepository.findByDeviceAndTimestampBetween(
                        device,
                        timestamp,
                        timestamp.plusMinutes(59)
                );

                if (existing.isEmpty()) {
                    // Simulate energy consumption based on power rating
                    double powerKW = device.getPowerRating() / 1000.0; // Convert W to kW
                    double energyConsumed = powerKW * 1.0; // 1 hour

                    // Add some realistic variation
                    energyConsumed = energyConsumed * (0.7 + Math.random() * 0.6); // 70-130% variation

                    EnergyLog log = new EnergyLog();
                    log.setDevice(device);
                    log.setUser(currentUser);
                    log.setTimestamp(timestamp);
                    log.setEnergyConsumed(energyConsumed);
                    log.setPowerWatts(device.getPowerRating());
                    log.setDurationMinutes(60);
                    log.setCostEstimate(energyConsumed * COST_PER_KWH);
                    log.setLogType("hourly");

                    energyLogRepository.save(log);
                }
            }
        }
    }

    // Generate daily summary
    @Transactional
    public void generateDailySummary() {
        User currentUser = getCurrentUser();
        LocalDateTime yesterday = LocalDateTime.now().minusDays(7);
        LocalDateTime startOfDay = yesterday.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = yesterday.toLocalDate().atTime(23, 59, 59);

        List<Device> devices = deviceRepository.findByOwner(currentUser);

        for (Device device : devices) {
            Double totalEnergy = energyLogRepository.getTotalEnergyByDeviceAndDateRange(
                    device, startOfDay, endOfDay
            );

            if (totalEnergy != null && totalEnergy > 0) {
                EnergyLog dailyLog = new EnergyLog();
                dailyLog.setDevice(device);
                dailyLog.setUser(currentUser);
                dailyLog.setTimestamp(startOfDay);
                dailyLog.setEnergyConsumed(totalEnergy);
                dailyLog.setPowerWatts(device.getPowerRating());
                dailyLog.setDurationMinutes(1440); // 24 hours
                dailyLog.setCostEstimate(totalEnergy * COST_PER_KWH);
                dailyLog.setLogType("daily");

                energyLogRepository.save(dailyLog);
            }
        }
    }

    // Helper method to map to response
    private EnergyLogResponse mapToLogResponse(EnergyLog log) {
        return new EnergyLogResponse(
                log.getId(),
                log.getDevice().getId(),
                log.getDevice().getName(),
                log.getDevice().getType(),
                log.getTimestamp(),
                log.getEnergyConsumed(),
                log.getPowerWatts(),
                log.getDurationMinutes(),
                log.getCostEstimate(),
                log.getLogType(),
                log.getCreatedAt()
        );
    }

    // Supporting classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphDataPoint {
        private String label;
        private Double value;
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceConsumption {
        private String deviceType;
        private Double totalEnergy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumptionSummary {
        private Double totalEnergy;
        private Double averageEnergy;
        private Double peakEnergy;
        private Double estimatedCost;
        private Integer activeDevices;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnergyLogResponse {
        private Long id;
        private Long deviceId;
        private String deviceName;
        private String deviceType;
        private LocalDateTime timestamp;
        private Double energyConsumed;
        private Double powerWatts;
        private Integer durationMinutes;
        private Double costEstimate;
        private String logType;
        private LocalDateTime createdAt;
    }
}
