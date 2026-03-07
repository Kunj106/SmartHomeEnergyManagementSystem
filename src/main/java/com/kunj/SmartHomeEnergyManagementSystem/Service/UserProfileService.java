package com.kunj.SmartHomeEnergyManagementSystem.Service;

import com.kunj.SmartHomeEnergyManagementSystem.DTO.UserProfileRequest;
import com.kunj.SmartHomeEnergyManagementSystem.DTO.UserProfileResponse;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.User;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.UserProfile;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserProfileRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService
{
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserProfileResponse createOrUpdateProfile(Long userId, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(new UserProfile());

        profile.setUser(user);
        updateProfileFromRequest(profile, request);

        UserProfile savedProfile = userProfileRepository.save(profile);
        return convertToResponse(savedProfile);
    }

    public UserProfileResponse getProfileByUserId(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user id: " + userId));

        return convertToResponse(profile);
    }

    @Transactional
    public void deleteProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user id: " + userId));

        userProfileRepository.delete(profile);
    }

    private void updateProfileFromRequest(UserProfile profile, UserProfileRequest request) {
        // Address Information
        profile.setAddressLine1(request.getAddressLine1());
        profile.setAddressLine2(request.getAddressLine2());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setPostalCode(request.getPostalCode());
        profile.setCountry(request.getCountry());

        // Household Information
        profile.setHouseholdSize(request.getHouseholdSize());
        profile.setPropertyType(request.getPropertyType());
        profile.setPropertySize(request.getPropertySize());
        profile.setNumberOfRooms(request.getNumberOfRooms());

        // Energy Information
        profile.setAverageMonthlyConsumption(request.getAverageMonthlyConsumption());

        // Preferences
        profile.setEnergySavingGoal(request.getEnergySavingGoal());
        profile.setTimezone(request.getTimezone());
    }

    private UserProfileResponse convertToResponse(UserProfile profile) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());

        // Address Information
        response.setAddressLine1(profile.getAddressLine1());
        response.setAddressLine2(profile.getAddressLine2());
        response.setCity(profile.getCity());
        response.setState(profile.getState());
        response.setPostalCode(profile.getPostalCode());
        response.setCountry(profile.getCountry());

        // Household Information
        response.setHouseholdSize(profile.getHouseholdSize());
        response.setPropertyType(profile.getPropertyType());
        response.setPropertySize(profile.getPropertySize());
        response.setNumberOfRooms(profile.getNumberOfRooms());

        // Energy Information
        response.setAverageMonthlyConsumption(profile.getAverageMonthlyConsumption());

        // Preferences
        response.setEnergySavingGoal(profile.getEnergySavingGoal());
        response.setTimezone(profile.getTimezone());

        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());

        return response;
    }
}
