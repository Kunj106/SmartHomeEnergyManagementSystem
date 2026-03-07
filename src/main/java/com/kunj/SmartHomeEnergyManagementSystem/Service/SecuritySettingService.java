package com.kunj.SmartHomeEnergyManagementSystem.Service;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.User;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SecuritySettingService
{
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuditLogService   auditLogService;
    @Autowired
    private SmsOtpService     smsOtpService;

    // Password change


     // Changes a user's password after verifying the current one.
     // @param userId          authenticated user's ID
     // @param currentPassword plain-text current password
     // @param newPassword     plain-text new password (min 8 chars enforced here)

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            auditLogService.logWarn(userId, "PASSWORD_CHANGE_FAILED",
                    "Incorrect current password supplied");
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.logInfo(userId, "PASSWORD_CHANGED",
                "Password changed successfully for user " + user.getUsername());
    }

    //  2FA enrollment flow
     // Step 1 – User submits their phone number and wants to enable 2FA.
     // We store the (unverified) phone, generate an OTP and SMS it.
     // The phone is NOT marked as verified yet.
    @Transactional
    public void initiate2FAEnrollment(Long userId, String phoneNumber) {
        // Temporarily save phone so SmsOtpService can find it
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTwoFactorPhone(phoneNumber);
        userRepository.save(user);

        boolean sent = smsOtpService.sendOtp(userId);
        if (!sent) {
            throw new RuntimeException("Failed to send verification SMS. Please check the phone number and try again.");
        }
    }

     // Step 2 – User submits the 6-digit OTP they received.
     // On success, 2FA is fully enabled.
    @Transactional
    public void confirm2FAEnrollment(Long userId, String otp) {
        boolean verified = smsOtpService.verifyOtp(userId, otp);
        if (!verified) {
            throw new IllegalArgumentException("Invalid or expired OTP. Please try again.");
        }
        // verifyOtp already marks token used; now officially enable 2FA
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        auditLogService.logInfo(userId, "2FA_ENROLLMENT_COMPLETE",
                "2FA enrollment completed for user " + user.getUsername());
    }

     // Disable 2FA for a user (requires current password as confirmation).
    @Transactional
    public void disable2FA(Long userId, String currentPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        smsOtpService.disable2FA(userId);
    }
}
