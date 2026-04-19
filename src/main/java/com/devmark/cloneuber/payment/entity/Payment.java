package com.devmark.cloneuber.payment.entity;

import com.devmark.cloneuber.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table (name = "payments")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn (name = "trip_id")
    private Trip trip;

    @Column
    private Double amount;

    @Column
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column
    private String paymentMethod;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime paidAt;

}
