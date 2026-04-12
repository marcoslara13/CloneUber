package com.devmark.cloneuber.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RatingRequest {
    private Long tripId;
    private Integer score;
    private String comment;
}
