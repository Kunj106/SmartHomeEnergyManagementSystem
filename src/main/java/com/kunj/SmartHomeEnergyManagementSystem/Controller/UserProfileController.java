package com.kunj.SmartHomeEnergyManagementSystem.Controller;

import com.kunj.SmartHomeEnergyManagementSystem.DTO.UserProfileRequest;
import com.kunj.SmartHomeEnergyManagementSystem.DTO.UserProfileResponse;
import com.kunj.SmartHomeEnergyManagementSystem.Security.UserDetailsImpl;
import com.kunj.SmartHomeEnergyManagementSystem.Service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/profile")
public class UserProfileController
{
    @Autowired
    private UserProfileService userProfileService;

    @PostMapping
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ResponseEntity<?> createOrUpdateProfile(@RequestBody UserProfileRequest request, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UserProfileResponse response = userProfileService.createOrUpdateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UserProfileResponse response = userProfileService.getProfileByUserId(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProfileByUserId(@PathVariable Long userId) {
        UserProfileResponse response = userProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('HOMEOWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteProfile(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        userProfileService.deleteProfile(userDetails.getId());
        return ResponseEntity.ok().body("Profile deleted successfully");
    }
}
