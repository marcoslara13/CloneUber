package com.devmark.cloneuber.trip.dto;

import com.devmark.cloneuber.trip.entity.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TripResponse {
    private Long id;
    private String passengerName;
    private String driverName;
    private String originAddress;
    private String destinationAddress;
    private TripStatus status;
    private Double estimatedPrice;
    private LocalDateTime requestedAt;
}
