package com.kunj.SmartHomeEnergyManagementSystem.Controller;

import com.kunj.SmartHomeEnergyManagementSystem.Security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/technician")
public class TechnicianController
{
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<?> getTechnicianDashboard(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok().body("Technician Dashboard - Welcome " + userDetails.getUsername());
    }

    @GetMapping("/installations")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<?> getInstallations(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok().body("Installation tasks for technician: " + userDetails.getUsername());
    }

    @PostMapping("/installations")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<?> createInstallation(@RequestBody String installationDetails, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok().body("Installation created by: " + userDetails.getUsername());
    }

    @GetMapping("/maintenance")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<?> getMaintenanceTasks(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok().body("Maintenance tasks for: " + userDetails.getUsername());
    }
}
