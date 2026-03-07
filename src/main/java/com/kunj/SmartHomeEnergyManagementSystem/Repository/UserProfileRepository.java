package com.kunj.SmartHomeEnergyManagementSystem.Repository;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile,Long>
{
    Optional<UserProfile> findByUserId(Long userId);
}
