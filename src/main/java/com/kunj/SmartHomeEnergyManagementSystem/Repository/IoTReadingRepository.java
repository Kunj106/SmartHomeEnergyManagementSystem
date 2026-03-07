package com.kunj.SmartHomeEnergyManagementSystem.Repository;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.Device;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.IoTReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IoTReadingRepository extends JpaRepository<IoTReading, Long> {

    // Get latest reading for a device
    Optional<IoTReading> findFirstByDeviceOrderByTimestampDesc(Device device);

    // Get latest reading by device ID
    @Query("SELECT r FROM IoTReading r WHERE r.device.id = :deviceId ORDER BY r.timestamp DESC LIMIT 1")
    Optional<IoTReading> findLatestByDeviceId(@Param("deviceId") Long deviceId);

    // Get all readings for a device within time range
    List<IoTReading> findByDeviceAndTimestampBetweenOrderByTimestampDesc(
            Device device,
            LocalDateTime start,
            LocalDateTime end
    );

    // Get readings for device in last N minutes
    @Query("SELECT r FROM IoTReading r WHERE r.device.id = :deviceId " +
            "AND r.timestamp >= :since ORDER BY r.timestamp DESC")
    List<IoTReading> findRecentByDeviceId(
            @Param("deviceId") Long deviceId,
            @Param("since") LocalDateTime since
    );

    // Get latest readings for all devices of a user
    @Query("SELECT r FROM IoTReading r WHERE r.device.owner.id = :userId " +
            "AND r.timestamp = (SELECT MAX(r2.timestamp) FROM IoTReading r2 " +
            "WHERE r2.device.id = r.device.id)")
    List<IoTReading> findLatestReadingsForUser(@Param("userId") Long userId);

    // Get latest readings for all active devices
    @Query("SELECT r FROM IoTReading r WHERE r.device.status = 'online' " +
            "AND r.device.owner.id = :userId " +
            "AND r.timestamp >= :since ORDER BY r.timestamp DESC")
    List<IoTReading> findLatestActiveDeviceReadings(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // Count anomalies for a device
    @Query("SELECT COUNT(r) FROM IoTReading r WHERE r.device.id = :deviceId " +
            "AND r.isAnomaly = true AND r.timestamp >= :since")
    Long countAnomaliesByDeviceId(
            @Param("deviceId") Long deviceId,
            @Param("since") LocalDateTime since
    );

    // Get average temperature for device
    @Query("SELECT AVG(r.temperature) FROM IoTReading r WHERE r.device.id = :deviceId " +
            "AND r.timestamp >= :since")
    Double getAverageTemperature(
            @Param("deviceId") Long deviceId,
            @Param("since") LocalDateTime since
    );

    // Get critical readings
    @Query("SELECT r FROM IoTReading r WHERE r.device.owner.id = :userId " +
            "AND r.status = 'CRITICAL' AND r.timestamp >= :since " +
            "ORDER BY r.timestamp DESC")
    List<IoTReading> findCriticalReadings(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // Delete old readings (for cleanup)
    void deleteByTimestampBefore(LocalDateTime timestamp);

    // Get readings aggregated by time interval
    @Query("SELECT DATE_TRUNC('minute', r.timestamp) as interval, " +
            "AVG(r.power) as avgPower, COUNT(DISTINCT r.device.id) as deviceCount " +
            "FROM IoTReading r WHERE r.device.owner.id = :userId " +
            "AND r.timestamp >= :since GROUP BY interval ORDER BY interval DESC")
    List<Object[]> getAggregatedPowerByMinute(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );
}