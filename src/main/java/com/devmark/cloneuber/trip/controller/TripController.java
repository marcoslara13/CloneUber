package com.devmark.cloneuber.trip.controller;

import com.devmark.cloneuber.common.response.ApiResponse;
import com.devmark.cloneuber.trip.dto.TripRequest;
import com.devmark.cloneuber.trip.dto.TripResponse;
import com.devmark.cloneuber.trip.entity.TripStatus;
import com.devmark.cloneuber.trip.service.TripService;
import com.devmark.cloneuber.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> requestTrip(
            @Valid @RequestBody TripRequest request,
            @AuthenticationPrincipal User user
            ){
        return ResponseEntity.ok(ApiResponse.ok(tripService.requestTrip(request, user)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TripResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam TripStatus status,
            @AuthenticationPrincipal User user
            ){
        return ResponseEntity.ok(ApiResponse.ok(tripService.updateStatus(id, status, user)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getMyTrips(
            @AuthenticationPrincipal User user
    ){
        return ResponseEntity.ok(ApiResponse.ok(tripService.getMyTrips(user)));
    }
}
