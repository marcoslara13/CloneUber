package com.devmark.cloneuber.trip.repository;

import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.trip.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByPassengerIdOrderByRequestedAtDesc(Long passengerId);
    List<Trip> findByDriverIdOrderByRequestedAtDesc(Long driverId);
    List<Trip> findByStatus(TripStatus status);
}
