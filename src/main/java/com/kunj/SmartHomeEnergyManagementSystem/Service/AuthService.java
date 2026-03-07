package com.kunj.SmartHomeEnergyManagementSystem.Service;

import com.kunj.SmartHomeEnergyManagementSystem.DTO.JwtResponse;
import com.kunj.SmartHomeEnergyManagementSystem.DTO.LoginRequest;
import com.kunj.SmartHomeEnergyManagementSystem.DTO.MessageResponse;
import com.kunj.SmartHomeEnergyManagementSystem.DTO.SignUpRequest;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.Role;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.RoleType;
import com.kunj.SmartHomeEnergyManagementSystem.Entity.User;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.RoleRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Security.JwtUtils;
import com.kunj.SmartHomeEnergyManagementSystem.Security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService
{
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Transactional
    public MessageResponse registerUser(SignUpRequest signUpRequest) {
        // Check if username exists
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        // Check if email exists
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // Create new user
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());

        // CRITICAL: Encode password before saving
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setEnabled(true);

        // Set roles
        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            // Default role: HOMEOWNER
            Role userRole = roleRepository.findByName(RoleType.ROLE_HOMEOWNER)
                    .orElseThrow(() -> new RuntimeException("Error: Role HOMEOWNER is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role ADMIN is not found."));
                        roles.add(adminRole);
                        break;
                    case "technician":
                        Role techRole = roleRepository.findByName(RoleType.ROLE_TECHNICIAN)
                                .orElseThrow(() -> new RuntimeException("Error: Role TECHNICIAN is not found."));
                        roles.add(techRole);
                        break;
                    default:
                        Role homeownerRole = roleRepository.findByName(RoleType.ROLE_HOMEOWNER)
                                .orElseThrow(() -> new RuntimeException("Error: Role HOMEOWNER is not found."));
                        roles.add(homeownerRole);
                }
            });
        }

        user.setRoles(roles);

        // Save user
        userRepository.save(user);

        return new MessageResponse("user registered successfully!!");
    }

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = jwtUtils.generateJwtToken(authentication);

        // Get user details
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // Return JWT response
        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );
    }
}
