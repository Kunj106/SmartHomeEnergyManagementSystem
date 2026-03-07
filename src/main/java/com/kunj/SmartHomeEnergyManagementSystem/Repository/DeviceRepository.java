package com.kunj.SmartHomeEnergyManagementSystem.Repository;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.Device;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    // Find all devices by owner/user
    List<Device> findByOwner(User owner);

    // Find device by ID and owner (for ownership verification)
    Optional<Device> findByIdAndOwner(Long id, User owner);

    // Find by owner ID
    @Query("SELECT d FROM Device d WHERE d.owner.id = :userId")
    List<Device> findByUserId(@Param("userId") Long userId);

    // Find by ID and user ID
    @Query("SELECT d FROM Device d WHERE d.id = :id AND d.owner.id = :userId")
    Optional<Device> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // ========== FILTER BY TYPE AND STATUS ==========

    // Find devices by type and owner
    List<Device> findByTypeAndOwner(String type, User owner);

    // Find devices by status and owner
    List<Device> findByStatusAndOwner(String status, User owner);

    // Find devices by owner and active status
    List<Device> findByOwnerAndIsActive(User owner, Boolean isActive);

    // Find devices by status (all users)
    List<Device> findByStatus(String status);

    // Find devices by type (all users)
    List<Device> findByType(String type);

    // Find devices by user and type
    @Query("SELECT d FROM Device d WHERE d.owner.id = :userId AND d.type = :type")
    List<Device> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    // ========== ACTIVE/ONLINE DEVICES ==========

    // Find all active (online) devices for a user
    @Query("SELECT d FROM Device d WHERE d.owner.id = :userId AND d.status = 'online'")
    List<Device> findActiveDevicesByUserId(@Param("userId") Long userId);

    // ========== SEARCH ==========

    // Search devices by name (case-insensitive) for a specific owner
    List<Device> findByOwnerAndNameContainingIgnoreCase(User owner, String name);

    // ========== COUNT METHODS ==========

    // Count total devices by owner
    Long countByOwner(User owner);

    // Count active devices by owner
    Long countByOwnerAndIsActive(User owner, Boolean isActive);

    // Count by user ID
    @Query("SELECT COUNT(d) FROM Device d WHERE d.owner.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    // Count by user ID and status
    @Query("SELECT COUNT(d) FROM Device d WHERE d.owner.id = :userId AND d.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    // Count active devices by type
    @Query("SELECT d.type, COUNT(d) FROM Device d WHERE d.owner.id = :userId " +
            "AND d.status = 'online' GROUP BY d.type")
    List<Object[]> countActiveDevicesByType(@Param("userId") Long userId);

    // Check if user has any devices
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Device d WHERE d.owner.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);

    // ========== POWER RATING / CAPACITY METHODS ==========

    // Get total power rating for all devices owned by a user
    @Query("SELECT SUM(d.powerRating) FROM Device d WHERE d.owner = :owner")
    Double getTotalPowerRatingByOwner(@Param("owner") User owner);

    // Sum power rating by user ID
    @Query("SELECT SUM(d.powerRating) FROM Device d WHERE d.owner.id = :userId")
    Double sumPowerRatingByUserId(@Param("userId") Long userId);

    // Sum power rating by user ID and status online
    @Query("SELECT SUM(d.powerRating) FROM Device d WHERE d.owner.id = :userId AND d.status = 'online'")
    Double sumPowerRatingByUserIdAndStatusOnline(@Param("userId") Long userId);

    // Get total installed capacity (sum of all device power ratings)
    @Query("SELECT COALESCE(SUM(d.powerRating), 0.0) FROM Device d WHERE d.owner.id = :userId")
    Double getTotalCapacity(@Param("userId") Long userId);

    // Find devices with high power rating (for load management)
    @Query("SELECT d FROM Device d WHERE d.owner.id = :userId " +
            "AND d.powerRating > :threshold ORDER BY d.powerRating DESC")
    List<Device> findHighPowerDevices(@Param("userId") Long userId,
                                      @Param("threshold") Double threshold);


}
