package com.devmark.cloneuber.trip.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TripRequest {
    @NotNull
    private Double originLat;
    @NotNull
    private Double originLon;
    private String originAddress;

    @NotNull
    private Double destinationLat;
    @NotNull
    private Double destinationLon;
    private String destinationAddress;
}
