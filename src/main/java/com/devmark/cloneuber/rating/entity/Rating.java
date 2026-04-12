package com.devmark.cloneuber.rating.entity;

import com.devmark.cloneuber.driver.entity.DriverProfile;
import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "ratings")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn (name = "trip_id")
    private Trip trip;

    @ManyToOne
    @JoinColumn (name = "rated_by_id")
    private User ratedBy;

    @ManyToOne
    @JoinColumn (name = "rated_to_id")
    private User ratedTo;

    @Column
    private Integer score;

    @Column
    private String comment;

    @Column
    private LocalDateTime timestamp;

}
