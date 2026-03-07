package com.kunj.SmartHomeEnergyManagementSystem.Controller;

import com.kunj.SmartHomeEnergyManagementSystem.DTO.*;
import com.kunj.SmartHomeEnergyManagementSystem.Security.UserDetailsImpl;
import com.kunj.SmartHomeEnergyManagementSystem.Service.IoTSimulationService;
import com.kunj.SmartHomeEnergyManagementSystem.Service.RealTimeMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Real-Time Monitoring", description = "Real-time power monitoring and IoT data endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class RealTimeMonitoringController
{
    private final RealTimeMonitoringService monitoringService;
    private final IoTSimulationService simulationService;


     // Get current real-time power consumption
     // GET /api/realtime/power
    @GetMapping("/power")
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get real-time power consumption",
            description = "Returns current total power consumption and device breakdown")
    public ResponseEntity<RealTimePowerDTO> getRealTimePower(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Fetching real-time power for user: {}", userDetails.getId());

        RealTimePowerDTO powerData = monitoringService.getRealTimePower(userDetails.getId());

        return ResponseEntity.ok(powerData);
    }

     // Get power trend over time
     // GET /api/realtime/trend?minutes=60
    @GetMapping("/trend")
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get power consumption trend",
            description = "Returns power consumption trend over specified time period")
    public ResponseEntity<List<PowerTrendDTO>> getPowerTrend(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "60") int minutes) {

        log.info("Fetching power trend for user: {} (last {} minutes)",
                userDetails.getId(), minutes);

        List<PowerTrendDTO> trend = monitoringService.getPowerTrend(
                userDetails.getId(), minutes);

        return ResponseEntity.ok(trend);
    }

     // Get chart data for real-time dashboard
     // GET /api/realtime/chart?dataPoints=20
    @GetMapping("/chart")
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get real-time chart data",
            description = "Returns formatted data for real-time power consumption charts")
    public ResponseEntity<RealTimeChartDataDTO> getChartData(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "20") int dataPoints) {

        log.info("Fetching chart data for user: {} ({} data points)",
                userDetails.getId(), dataPoints);

        RealTimeChartDataDTO chartData = monitoringService
                .getRealTimeChartData(userDetails.getId(), dataPoints);

        return ResponseEntity.ok(chartData);
    }


     // Get consumption by device type
     // GET /api/realtime/by-type
    @GetMapping("/by-type")
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get consumption by device type",
            description = "Returns power consumption breakdown by device type")
    public ResponseEntity<List<DeviceTypeConsumptionDTO>> getConsumptionByType(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Fetching consumption by type for user: {}", userDetails.getId());

        List<DeviceTypeConsumptionDTO> consumption = monitoringService
                .getConsumptionByDeviceType(userDetails.getId());

        return ResponseEntity.ok(consumption);
    }

     // Get latest IoT readings for a device
     // GET /api/realtime/device/{deviceId}/readings?limit=50
    @GetMapping("/device/{deviceId}/readings")
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get device IoT readings",
            description = "Returns latest IoT sensor readings for a specific device")
    public ResponseEntity<List<IoTReadingDTO>> getDeviceReadings(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "50") int limit) {

        log.info("Fetching readings for device: {} (limit: {})", deviceId, limit);

        List<IoTReadingDTO> readings = monitoringService
                .getDeviceReadings(deviceId, limit);

        return ResponseEntity.ok(readings);
    }

     // Get device health status
     // GET /api/realtime/device/{deviceId}/health
    @GetMapping("/device/{deviceId}/health")
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Get device health status",
            description = "Returns health metrics and diagnostics for a device")
    public ResponseEntity<DeviceHealthDTO> getDeviceHealth(
            @PathVariable Long deviceId) {

        log.info("Fetching health status for device: {}", deviceId);

        DeviceHealthDTO health = monitoringService.getDeviceHealth(deviceId);

        return ResponseEntity.ok(health);
    }

     // Get power alerts
     // GET /api/realtime/alerts
    @GetMapping("/alerts")
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get power alerts",
            description = "Returns recent power consumption alerts and warnings")
    public ResponseEntity<List<PowerAlertDTO>> getPowerAlerts(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Fetching alerts for user: {}", userDetails.getId());

        List<PowerAlertDTO> alerts = monitoringService
                .getPowerAlerts(userDetails.getId());

        return ResponseEntity.ok(alerts);
    }

     // Manually trigger IoT data generation (for testing)
     // POST /api/realtime/simulate/now
    @PostMapping("/simulate/now")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate IoT readings now",
            description = "Manually triggers generation of simulated IoT readings (Admin only)")
    public ResponseEntity<String> simulateNow() {

        log.info("Manual trigger: Generating IoT readings");

        simulationService.generateReadingsNow();

        return ResponseEntity.ok("IoT readings generated successfully");
    }


     // Generate historical data for testing
     // POST /api/realtime/simulate/historical?deviceId=1&hours=24
    @PostMapping("/simulate/historical")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate historical IoT data",
            description = "Generates historical IoT readings for testing/demo (Admin only)")
    public ResponseEntity<String> generateHistoricalData(
            @RequestParam Long deviceId,
            @RequestParam(defaultValue = "24") int hours) {

        log.info("Generating {} hours of historical data for device: {}",
                hours, deviceId);

        simulationService.generateHistoricalData(deviceId, hours);

        return ResponseEntity.ok(
                String.format("Generated %d hours of historical data", hours));
    }
}
