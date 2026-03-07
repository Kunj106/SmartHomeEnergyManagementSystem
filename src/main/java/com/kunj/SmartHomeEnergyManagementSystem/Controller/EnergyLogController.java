package com.kunj.SmartHomeEnergyManagementSystem.Controller;

import com.kunj.SmartHomeEnergyManagementSystem.DTO.EnergyLogRequest;
import com.kunj.SmartHomeEnergyManagementSystem.Service.EnergyLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/energy")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EnergyLogController
{
    @Autowired
    private EnergyLogService energyLogService;

    // Create energy log
    @PostMapping("/logs")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> createEnergyLog(@Valid @RequestBody EnergyLogRequest request) {
        try {
            EnergyLogService.EnergyLogResponse log = energyLogService.createEnergyLog(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(log);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get hourly consumption for graphs (today)
    @GetMapping("/graphs/hourly")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getHourlyConsumption() {
        try {
            List<EnergyLogService.GraphDataPoint> data = energyLogService.getHourlyConsumption();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get daily consumption for graphs (last 30 days)
    @GetMapping("/graphs/daily")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getDailyConsumption() {
        try {
            List<EnergyLogService.GraphDataPoint> data = energyLogService.getDailyConsumption();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get consumption by device type
    @GetMapping("/consumption/by-device")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getConsumptionByDevice() {
        try {
            List<EnergyLogService.DeviceConsumption> data = energyLogService.getConsumptionByDevice();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get consumption summary
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getConsumptionSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            if (start == null) {
                start = LocalDateTime.now().minusDays(30);
            }
            if (end == null) {
                end = LocalDateTime.now();
            }
            EnergyLogService.ConsumptionSummary summary = energyLogService.getConsumptionSummary(start, end);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Get logs for specific device
    @GetMapping("/logs/device/{deviceId}")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> getDeviceLogs(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            List<EnergyLogService.EnergyLogResponse> logs = energyLogService.getDeviceLogs(deviceId, start, end);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Generate hourly logs (for testing/simulation)
    @PostMapping("/logs/generate-hourly")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> generateHourlyLogs() {
        try {
            energyLogService.generateHourlyLogs();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Hourly logs generated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Generate daily summary
    @PostMapping("/logs/generate-daily")
    @PreAuthorize("hasAnyRole('HOMEOWNER', 'ADMIN')")
    public ResponseEntity<?> generateDailySummary() {
        try {
            energyLogService.generateDailySummary();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Daily summary generated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Helper method
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
