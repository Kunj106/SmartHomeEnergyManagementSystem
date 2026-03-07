package com.kunj.SmartHomeEnergyManagementSystem.Repository;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.Role;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role , Long>
{
    Optional<Role> findByName(RoleType name);
}
