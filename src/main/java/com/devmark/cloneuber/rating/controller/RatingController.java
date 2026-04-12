package com.devmark.cloneuber.rating.controller;

import com.devmark.cloneuber.common.response.ApiResponse;
import com.devmark.cloneuber.rating.dto.RatingRequest;
import com.devmark.cloneuber.rating.dto.RatingResponse;
import com.devmark.cloneuber.rating.service.RatingService;
import com.devmark.cloneuber.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<ApiResponse<RatingResponse>> rateTrip(
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal User ratedBy
            ){
        return ResponseEntity.ok(ApiResponse.ok(ratingService.rateTrip(request, ratedBy)));
    }

    @GetMapping("/user/{userId}/average")
    public ResponseEntity<ApiResponse<Double>> getAverageScore(
            @PathVariable Long userId
    ){
        return ResponseEntity.ok(ApiResponse.ok(ratingService.getAverageScore(userId)));
    }
}
