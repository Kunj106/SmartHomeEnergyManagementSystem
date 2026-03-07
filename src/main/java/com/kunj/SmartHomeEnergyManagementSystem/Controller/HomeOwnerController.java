package com.kunj.SmartHomeEnergyManagementSystem.Controller;

import com.kunj.SmartHomeEnergyManagementSystem.Security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/homeowner")
public class HomeOwnerController
{
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<?> getHomeownerDashboard(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok().body("Homeowner Dashboard - Welcome " + userDetails.getUsername());
    }

    @GetMapping("/devices")
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<?> getDevices(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok().body("Devices managed by: " + userDetails.getUsername());
    }

    @PostMapping("/devices")
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<?> addDevice(@RequestBody String deviceName, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok().body("Device added by homeowner: " + userDetails.getUsername());
    }

    @GetMapping("/energy-usage")
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<?> getEnergyUsage(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok().body("Energy usage data for: " + userDetails.getUsername());
    }
}
