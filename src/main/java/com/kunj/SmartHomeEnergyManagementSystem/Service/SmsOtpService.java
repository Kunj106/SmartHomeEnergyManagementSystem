package com.kunj.SmartHomeEnergyManagementSystem.Service;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.OtpToken;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.User;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.OtpTokenRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class SmsOtpService
{
    // Twilio credentials injected from application.properties
    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    // OTP expiry window (minutes)
    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final AuditLogService    auditLogService;
    private final SecureRandom random = new SecureRandom();

    public SmsOtpService(OtpTokenRepository otpTokenRepository,
                         UserRepository userRepository,
                         AuditLogService auditLogService) {
        this.otpTokenRepository = otpTokenRepository;
        this.userRepository     = userRepository;
        this.auditLogService    = auditLogService;
    }

    @PostConstruct
    public void initTwilio() {
        Twilio.init(twilioAccountSid, twilioAuthToken);
        log.info("Twilio SDK initialised.");
    }

     // Generate a 6-digit OTP, persist it, send it via Twilio SMS to the user's
     // twoFactorPhone number, and return whether the SMS was dispatched.
    @Transactional
    public boolean sendOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String phone = user.getTwoFactorPhone();
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("No 2FA phone number set for this user. " +
                    "Please update your profile with a phone number first.");
        }

        // Clean up stale tokens before issuing a fresh one
        otpTokenRepository.deleteExpiredOrUsedForUser(userId, LocalDateTime.now());

        String otp = generateOtp();

        OtpToken token = OtpToken.builder()
                .user(user)
                .token(otp)
                .phoneNumber(phone)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();
        otpTokenRepository.save(token);

        boolean sent = sendSms(phone, buildSmsBody(otp));

        if (sent) {
            auditLogService.logInfo(userId, "OTP_SENT",
                    "2FA OTP sent to " + maskPhone(phone));
        } else {
            auditLogService.logError(userId, "OTP_SEND_FAILED",
                    "Failed to send 2FA OTP to " + maskPhone(phone));
        }

        return sent;
    }

     // Verify the submitted OTP for a user.
     // Returns true on success and marks the token as used.

    @Transactional
    public boolean verifyOtp(Long userId, String submittedOtp) {
        Optional<OtpToken> tokenOpt = otpTokenRepository
                .findValidOtpByTokenAndUserId(submittedOtp.trim(), userId, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            auditLogService.logWarn(userId, "OTP_VERIFY_FAILED",
                    "Invalid or expired OTP submitted");
            return false;
        }

        OtpToken token = tokenOpt.get();
        token.setUsed(true);
        otpTokenRepository.save(token);

        auditLogService.logInfo(userId, "OTP_VERIFIED", "2FA OTP verified successfully");
        return true;
    }

     // Enable 2FA for a user after they have verified their phone via OTP.
     // Sets twoFactorEnabled = true and persists the verified phone number.
    @Transactional
    public void enable2FA(Long userId, String phoneNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setTwoFactorEnabled(true);
        user.setTwoFactorPhone(normalisePhone(phoneNumber));
        userRepository.save(user);
        auditLogService.logInfo(userId, "2FA_ENABLED",
                "Two-factor authentication enabled for user " + user.getUsername());
    }

     // Disable 2FA for a user.
    @Transactional
    public void disable2FA(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        auditLogService.logWarn(userId, "2FA_DISABLED",
                "Two-factor authentication disabled for user " + user.getUsername());
    }

     // Return the current 2FA status of a user.
    public TwoFAStatusDTO get2FAStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return new TwoFAStatusDTO(
                Boolean.TRUE.equals(user.getTwoFactorEnabled()),
                user.getTwoFactorPhone() != null ? maskPhone(user.getTwoFactorPhone()) : null
        );
    }

    // Private helpers

    private String generateOtp() {
        int code = 100000 + random.nextInt(900000); // 100000 – 999999
        return String.valueOf(code);
    }

    private String buildSmsBody(String otp) {
        return "Your Smart Energy 2FA code is: " + otp +
                ". It expires in " + otpExpiryMinutes + " minutes. Do not share this code.";
    }

    private boolean sendSms(String toPhone, String body) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhone),
                    new PhoneNumber(twilioPhoneNumber),
                    body
            ).create();
            log.info("SMS sent. SID={}", message.getSid());
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", maskPhone(toPhone), e.getMessage());
            return false;
        }
    }

    private String normalisePhone(String phone) {
        phone = phone.trim().replaceAll("[\\s\\-()]", "");
        return phone.startsWith("+") ? phone : "+" + phone;
    }

    // Mask phone for logging
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, Math.min(4, phone.length() - 4))
                + "******"
                + phone.substring(phone.length() - 4);
    }

    //  DTOs
    public record TwoFAStatusDTO(boolean enabled, String maskedPhone) {}
}
