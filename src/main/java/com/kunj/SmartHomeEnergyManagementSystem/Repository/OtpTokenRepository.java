package com.kunj.SmartHomeEnergyManagementSystem.Repository;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken,Long>
{
    // Find the latest unused, non-expired OTP for a user
    @Query("SELECT o FROM OtpToken o WHERE o.user.id = :userId AND o.used = false " +
            "AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<OtpToken> findLatestValidOtp(@Param("userId") Long userId,
                                          @Param("now") LocalDateTime now);

    // Find by token string (for verification)
    @Query("SELECT o FROM OtpToken o WHERE o.token = :token AND o.user.id = :userId " +
            "AND o.used = false AND o.expiresAt > :now")
    Optional<OtpToken> findValidOtpByTokenAndUserId(@Param("token") String token,
                                                    @Param("userId") Long userId,
                                                    @Param("now") LocalDateTime now);

    // Clean up old / expired tokens for a user (called before generating a new one)
    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.user.id = :userId AND (o.used = true OR o.expiresAt < :now)")
    void deleteExpiredOrUsedForUser(@Param("userId") Long userId,
                                    @Param("now") LocalDateTime now);
}
