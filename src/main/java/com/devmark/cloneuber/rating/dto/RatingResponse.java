package com.devmark.cloneuber.rating.dto;

import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RatingResponse {
    private Long ratingId;
    private Long tripId;
    private String ratedByName;
    private String ratedToName;
    private Integer score;
    private String comment;
    private LocalDateTime timestamp;
}
