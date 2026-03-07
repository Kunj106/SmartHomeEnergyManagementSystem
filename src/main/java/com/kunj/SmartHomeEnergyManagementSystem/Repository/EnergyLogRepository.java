package com.kunj.SmartHomeEnergyManagementSystem.Repository;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.Device;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.EnergyLog;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EnergyLogRepository extends JpaRepository<EnergyLog,Long>
{
    // Find logs by device
    List<EnergyLog> findByDevice(Device device);

    // Find logs by user
    List<EnergyLog> findByUser(User user);

    // Find logs by device and date range
    List<EnergyLog> findByDeviceAndTimestampBetween(Device device, LocalDateTime start, LocalDateTime end);

    // Find logs by user and date range
    List<EnergyLog> findByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);

    // Find hourly logs for device
    List<EnergyLog> findByDeviceAndLogTypeOrderByTimestampDesc(Device device, String logType);

    // Get total energy consumed by device in date range
    @Query("SELECT SUM(e.energyConsumed) FROM EnergyLog e WHERE e.device = :device AND e.timestamp BETWEEN :start AND :end")
    Double getTotalEnergyByDeviceAndDateRange(@Param("device") Device device, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Get total energy consumed by user in date range
    @Query("SELECT SUM(e.energyConsumed) FROM EnergyLog e WHERE e.user = :user AND e.timestamp BETWEEN :start AND :end")
    Double getTotalEnergyByUserAndDateRange(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Get hourly consumption for today by user
    @Query("SELECT e FROM EnergyLog e WHERE e.user = :user AND e.logType = 'hourly' AND DATE(e.timestamp) = CURRENT_DATE ORDER BY e.timestamp ASC")
    List<EnergyLog> getTodayHourlyLogsByUser(@Param("user") User user);

    // Get daily consumption for last 30 days by user
    @Query("SELECT e FROM EnergyLog e WHERE e.user = :user AND e.logType = 'daily' AND e.timestamp >= :startDate ORDER BY e.timestamp ASC")
    List<EnergyLog> getDailyLogsForLast30Days(@Param("user") User user, @Param("startDate") LocalDateTime startDate);

    // Get consumption by device type
    @Query("SELECT d.type, SUM(e.energyConsumed) FROM EnergyLog e JOIN e.device d WHERE e.user = :user AND e.timestamp BETWEEN :start AND :end GROUP BY d.type")
    List<Object[]> getEnergyByDeviceType(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Get average daily consumption
    @Query("SELECT AVG(e.energyConsumed) FROM EnergyLog e WHERE e.user = :user AND e.logType = 'daily' AND e.timestamp >= :startDate")
    Double getAverageDailyConsumption(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
}
