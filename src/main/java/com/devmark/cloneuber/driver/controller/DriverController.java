package com.devmark.cloneuber.driver.controller;

import com.devmark.cloneuber.common.response.ApiResponse;
import com.devmark.cloneuber.driver.entity.DriverProfile;
import com.devmark.cloneuber.driver.repository.DriverProfileRepository;
import com.devmark.cloneuber.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverProfileRepository driverProfileRepository;

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<String>> createProfile(
            @AuthenticationPrincipal User user,
            @RequestParam String vehiclePlate,
            @RequestParam String vehicleModel,
            @RequestParam String vehicleColor) {

        DriverProfile profile = DriverProfile.builder()
                .user(user)
                .vehiclePlate(vehiclePlate)
                .vehicleModel(vehicleModel)
                .vehicleColor(vehicleColor)
                .available(false)
                .currentLat(0.0)
                .currentLon(0.0)
                .build();

        driverProfileRepository.save(profile);
        return ResponseEntity.ok(ApiResponse.ok("Perfil de conductor creado"));
    }

    @PatchMapping("/availability")
    public ResponseEntity<ApiResponse<String>> setAvailability(
            @AuthenticationPrincipal User user,
            @RequestParam Boolean available,
            @RequestParam Double lat,
            @RequestParam Double lng) {

        DriverProfile profile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de conductor no encontrado"));

        profile.setAvailable(available);
        profile.setCurrentLat(lat);
        profile.setCurrentLon(lng);
        driverProfileRepository.save(profile);

        return ResponseEntity.ok(ApiResponse.ok("Disponibilidad actualizada"));
    }
}