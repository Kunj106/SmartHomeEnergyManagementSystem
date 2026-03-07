package com.kunj.SmartHomeEnergyManagementSystem.Service;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.Device;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.IoTReading;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.ReadingStatus;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.DeviceRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.IoTReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class IoTSimulationService
{
    private final DeviceRepository deviceRepository;
    private final IoTReadingRepository iotReadingRepository;
    private final Random random = new Random();


     // Simulates real-time sensor readings for all active devices
     // Runs every 30 seconds (configurable)
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    public void generateSimulatedReadings() {
        log.info("Generating simulated IoT readings...");

        List<Device> activeDevices = deviceRepository.findByStatus("online");

        for (Device device : activeDevices) {
            try {
                IoTReading reading = generateReadingForDevice(device);
                iotReadingRepository.save(reading);
                log.debug("Generated reading for device: {}", device.getName());
            } catch (Exception e) {
                log.error("Error generating reading for device {}: {}",
                        device.getId(), e.getMessage());
            }
        }

        log.info("Generated {} IoT readings", activeDevices.size());
    }


     // Generate a realistic reading for a specific device
    private IoTReading generateReadingForDevice(Device device) {
        // Base values depend on device type and power rating
        Double basePower = device.getPowerRating();
        Double baseVoltage = device.getVoltageRating() != null ?
                device.getVoltageRating() : 230.0; // Default to 230V

        // Add realistic variations
        Double voltage = generateVoltage(baseVoltage);
        Double powerVariation = generatePowerVariation(device.getType());
        Double actualPower = basePower * powerVariation;
        Double current = actualPower / voltage; // P = V * I

        // Get previous reading for energy calculation
        Double cumulativeEnergy = calculateCumulativeEnergy(device, actualPower);

        return IoTReading.builder()
                .device(device)
                .voltage(voltage)
                .current(current)
                .power(actualPower)
                .energy(cumulativeEnergy)
                .powerFactor(generatePowerFactor())
                .frequency(generateFrequency())
                .temperature(generateTemperature(device.getType()))
                .status(determineStatus(actualPower, basePower))
                .timestamp(LocalDateTime.now())
                .isAnomaly(detectAnomaly(actualPower, basePower))
                .dataSource("SIMULATED")
                .build();
    }


     // Generate realistic voltage with small variations
    private Double generateVoltage(Double baseVoltage) {
        // Voltage typically varies by ±5%
        double variation = 0.95 + (random.nextDouble() * 0.1); // 0.95 to 1.05
        return Math.round(baseVoltage * variation * 10.0) / 10.0;
    }

     // Generate power variation based on device type
     // Different devices have different usage patterns
    private Double generatePowerVariation(String deviceType) {
        double baseVariation = 0.7 + (random.nextDouble() * 0.3); // 70-100% of rated power

        // Device-specific patterns
        switch (deviceType.toLowerCase()) {
            case "air conditioner":
                // AC varies more based on compressor cycling
                return 0.6 + (random.nextDouble() * 0.4); // 60-100%

            case "refrigerator":
                // Refrigerator has cyclic pattern
                return random.nextDouble() < 0.3 ? 0.2 : 0.8; // 20% or 80%

            case "water heater":
                // Water heater either on or off
                return random.nextDouble() < 0.4 ? 0.0 : 0.95; // 0% or 95%

            case "washing machine":
                // Washing machine varies by cycle
                double[] washingCycles = {0.3, 0.5, 0.8, 1.0};
                return washingCycles[random.nextInt(washingCycles.length)];

            case "television":
            case "microwave":
                // Steady consumption
                return 0.85 + (random.nextDouble() * 0.15); // 85-100%

            default:
                return baseVariation;
        }
    }


     // Calculate cumulative energy consumption
    private Double calculateCumulativeEnergy(Device device, Double currentPower) {
        // Get last reading
        var lastReading = iotReadingRepository.findLatestByDeviceId(device.getId());

        if (lastReading.isEmpty()) {
            // First reading - start at 0
            return currentPower / 1000.0 * (30.0 / 3600.0); // Convert 30 seconds to kWh
        }

        // Add to previous cumulative energy
        Double previousEnergy = lastReading.get().getEnergy();
        Double energyIncrement = currentPower / 1000.0 * (30.0 / 3600.0); // 30 seconds in hours

        return Math.round((previousEnergy + energyIncrement) * 1000.0) / 1000.0;
    }


     // Generate realistic power factor (typically 0.85-0.98)
    private Double generatePowerFactor() {
        return 0.85 + (random.nextDouble() * 0.13);
    }

     // Generate frequency (50Hz or 60Hz with small variation)
    private Double generateFrequency() {
        double baseFreq = 50.0; // or 60.0 for US
        return baseFreq + (random.nextDouble() * 0.2 - 0.1); // ±0.1 Hz
    }


     // Generate realistic device temperature
    private Double generateTemperature(String deviceType) {
        switch (deviceType.toLowerCase()) {
            case "air conditioner":
                return 35.0 + (random.nextDouble() * 15.0); // 35-50°C

            case "refrigerator":
                return 40.0 + (random.nextDouble() * 10.0); // 40-50°C

            case "water heater":
                return 50.0 + (random.nextDouble() * 20.0); // 50-70°C

            case "washing machine":
                return 30.0 + (random.nextDouble() * 15.0); // 30-45°C

            default:
                return 25.0 + (random.nextDouble() * 15.0); // 25-40°C
        }
    }

     // Determine reading status based on power consumption
    private ReadingStatus determineStatus(Double actualPower, Double ratedPower) {
        double ratio = actualPower / ratedPower;

        if (ratio > 1.2) {
            return ReadingStatus.CRITICAL; // Over 120% capacity
        } else if (ratio > 1.05) {
            return ReadingStatus.WARNING; // Over 105% capacity
        } else {
            return ReadingStatus.NORMAL;
        }
    }


     // Detect anomalies in power consumption
    private Boolean detectAnomaly(Double actualPower, Double ratedPower) {
        double ratio = actualPower / ratedPower;

        // Flag as anomaly if:
        // 1. Power exceeds rated capacity by 15%
        // 2. Random 2% chance (simulates sensor errors)
        return ratio > 1.15 || random.nextDouble() < 0.02;
    }

     // Manual trigger for generating readings (useful for testing)
    @Transactional
    public void generateReadingsNow() {
        log.info("Manual trigger: Generating readings now...");
        generateSimulatedReadings();
    }


     // Generate historical data for a device (for testing/demo)
    @Transactional
    public void generateHistoricalData(Long deviceId, int hours) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);

        // Generate readings every 5 minutes for the past N hours
        for (int i = 0; i < hours * 12; i++) {
            LocalDateTime readingTime = startTime.plusMinutes(i * 5);

            IoTReading reading = generateReadingForDevice(device);
            reading.setTimestamp(readingTime);

            iotReadingRepository.save(reading);
        }

        log.info("Generated {} historical readings for device {}",
                hours * 12, device.getName());
    }


      //Cleanup old readings (optional - runs daily)
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupOldReadings() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30); // Keep 30 days
        iotReadingRepository.deleteByTimestampBefore(cutoff);
        log.info("Cleaned up IoT readings older than {}", cutoff);
    }
}
