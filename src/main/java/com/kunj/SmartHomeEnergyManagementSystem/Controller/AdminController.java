package com.kunj.SmartHomeEnergyManagementSystem.Controller;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.User;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Security.UserDetailsImpl;
import com.kunj.SmartHomeEnergyManagementSystem.Service.AuditLogService;
import com.kunj.SmartHomeEnergyManagementSystem.Service.ReportService;
import com.kunj.SmartHomeEnergyManagementSystem.Service.SecuritySettingService;
import com.kunj.SmartHomeEnergyManagementSystem.Service.SmsOtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private UserRepository          userRepository;
    @Autowired private AuditLogService         auditLogService;
    @Autowired private ReportService           reportService;
    @Autowired private SecuritySettingService securitySettingsService;
    @Autowired private SmsOtpService           smsOtpService;

    //  MANAGE USERS

    // get all users
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal UserDetailsImpl me) {
        List<User> users = userRepository.findAll();
        auditLogService.logInfo(me.getId(), "USER_LIST_FETCHED",
                "Admin fetched all users (" + users.size() + " records)");
        return ResponseEntity.ok(users);
    }

    // Get single user
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetailsImpl me) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        auditLogService.logInfo(me.getId(), "USER_FETCHED", "Fetched user id=" + id);
        return ResponseEntity.ok(user);
    }

    // Update user admin fields
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody Map<String, Object> updates,
                                        @AuthenticationPrincipal UserDetailsImpl me) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        if (updates.containsKey("enabled")) {
            user.setEnabled(Boolean.parseBoolean(updates.get("enabled").toString()));
        }
        if (updates.containsKey("firstName")) {
            user.setFirstName(updates.get("firstName").toString());
        }
        if (updates.containsKey("lastName")) {
            user.setLastName(updates.get("lastName").toString());
        }
        if (updates.containsKey("phoneNumber")) {
            user.setPhoneNumber(updates.get("phoneNumber").toString());
        }

        userRepository.save(user);
        auditLogService.logInfo(me.getId(), "USER_UPDATED",
                "Admin updated user id=" + id, "User", id, null);
        return ResponseEntity.ok(user);
    }

   // Delete user
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetailsImpl me) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(error("User not found: " + id));
        }
        userRepository.deleteById(id);
        auditLogService.logWarn(me.getId(), "USER_DELETED",
                "Admin deleted user id=" + id, "User", id, null);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // Enable / disable a user account
    @PatchMapping("/users/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUser(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetailsImpl me) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
        userRepository.save(user);
        String action = Boolean.TRUE.equals(user.getEnabled()) ? "USER_ENABLED" : "USER_DISABLED";
        auditLogService.logWarn(me.getId(), action, "User id=" + id + " status toggled");
        return ResponseEntity.ok(Map.of("enabled", user.getEnabled(), "message", "User status updated"));
    }

    //  VIEW LOGS

     // GET /api/admin/logs?page=0&size=50&level=WARN&action=LOGIN
     // All params optional; returns paginated log entries.
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getLogs(
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "50")   int    size,
            @RequestParam(required = false)      String level,
            @RequestParam(required = false)      String action,
            @AuthenticationPrincipal UserDetailsImpl me) {

        Page<AuditLogService.AuditLogDTO> logs;

        if (level != null && !level.isBlank()) {
            logs = auditLogService.getLogsByLevel(level.toUpperCase(), page, size);
        } else if (action != null && !action.isBlank()) {
            logs = auditLogService.getLogsByAction(action, page, size);
        } else {
            logs = auditLogService.getAllLogs(page, size);
        }

        auditLogService.logInfo(me.getId(), "LOGS_VIEWED",
                "Admin viewed audit logs (page=" + page + ", size=" + size + ")");

        Map<String, Object> response = new HashMap<>();
        response.put("content",       logs.getContent());
        response.put("totalElements", logs.getTotalElements());
        response.put("totalPages",    logs.getTotalPages());
        response.put("currentPage",   page);
        response.put("summary",       auditLogService.getSummary());
        return ResponseEntity.ok(response);
    }

    //  GENERATE REPORT
    /** GET /api/admin/report/json — returns full system report as JSON */
    @GetMapping("/report/json")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateReportJson(@AuthenticationPrincipal UserDetailsImpl me) {
        ReportService.ReportData report = reportService.generateSystemReport(me.getId());
        return ResponseEntity.ok(report);
    }

    /** GET /api/admin/report/csv — streams the report as a downloadable CSV file */
    @GetMapping("/report/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> generateReportCsv(@AuthenticationPrincipal UserDetailsImpl me) {
        ReportService.ReportData report = reportService.generateSystemReport(me.getId());
        String csv = reportService.toCsv(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"smart_energy_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    //  SECURITY SETTINGS — Password & 2FA

     // POST /api/admin/security/change-password
    @PostMapping("/security/change-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal UserDetailsImpl me) {
        try {
            securitySettingsService.changePassword(
                    me.getId(),
                    body.get("currentPassword"),
                    body.get("newPassword")
            );
            return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

     // POST /api/admin/security/2fa/initiate
     // Sends OTP to the supplied number.
    @PostMapping("/security/2fa/initiate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> initiate2FA(@RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal UserDetailsImpl me) {
        try {
            securitySettingsService.initiate2FAEnrollment(me.getId(), body.get("phoneNumber"));
            return ResponseEntity.ok(Map.of("message",
                    "OTP sent to your phone. Please verify to complete 2FA setup."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

     // POST /api/admin/security/2fa/verify
     // Verifies OTP and fully enables 2FA.
    @PostMapping("/security/2fa/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verify2FA(@RequestBody Map<String, String> body,
                                       @AuthenticationPrincipal UserDetailsImpl me) {
        try {
            securitySettingsService.confirm2FAEnrollment(me.getId(), body.get("otp"));
            return ResponseEntity.ok(Map.of("message", "Two-factor authentication enabled successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

     // POST /api/admin/security/2fa/disable
    @PostMapping("/security/2fa/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disable2FA(@RequestBody Map<String, String> body,
                                        @AuthenticationPrincipal UserDetailsImpl me) {
        try {
            securitySettingsService.disable2FA(me.getId(), body.get("currentPassword"));
            return ResponseEntity.ok(Map.of("message", "Two-factor authentication disabled."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    /** GET /api/admin/security/2fa/status — returns current 2FA state */
    @GetMapping("/security/2fa/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> get2FAStatus(@AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(smsOtpService.get2FAStatus(me.getId()));
    }

    //  DASHBOARD
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminDashboard() {
        return ResponseEntity.ok("Admin Dashboard - System Settings and Management");
    }

    //Helpers

    private Map<String, String> error(String msg) {
        return Map.of("error", msg);
    }


    private void auditInfo(Long userId, String action, String description,
                           String entityType, Long entityId, String ip) {
        auditLogService.logAction(userId, action, description, "INFO", entityType, entityId, ip);
    }
}