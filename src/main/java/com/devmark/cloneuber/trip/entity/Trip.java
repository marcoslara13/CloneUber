package com.devmark.cloneuber.trip.entity;


import com.devmark.cloneuber.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table (name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    private Double originLat;
    private Double originLon;
    private String originAddress;

    private Double destinationLat;
    private Double destinationLon;
    private String destinationAddress;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private Double estimatedPrice;
    private Double finalPrice;

    @CreationTimestamp
    private LocalDateTime requestedAt;

    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
