package com.devmark.cloneuber.driver.entity;


import com.devmark.cloneuber.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String vehiclePlate;
    private String vehicleModel;
    private String vehicleColor;

    private Boolean available;

    private Double currentLat;
    private Double currentLon;

    @UpdateTimestamp
    private LocalDateTime locationUpdatedAt;
}
