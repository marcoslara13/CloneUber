package com.devmark.cloneuber.trip.service;

import com.devmark.cloneuber.driver.entity.DriverProfile;
import com.devmark.cloneuber.driver.repository.DriverProfileRepository;
import com.devmark.cloneuber.trip.dto.TripRequest;
import com.devmark.cloneuber.trip.dto.TripResponse;
import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.trip.entity.TripStatus;
import com.devmark.cloneuber.trip.repository.TripRepository;
import com.devmark.cloneuber.user.entity.Role;
import com.devmark.cloneuber.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TripService {
    private final TripRepository tripRepository;
    private final DriverProfileRepository driverProfileRepository;

    public TripResponse requestTrip(TripRequest request, User passenger){
        double distance = calculateDistance(request.getOriginLat(), request.getOriginLon(),
                request.getDestinationLat(), request.getDestinationLon());
        double estimatedPrice = Math.max(3.0, distance * 1.5);

        List<DriverProfile> availableDrivers = driverProfileRepository.findByAvailableTrue();
        DriverProfile nearestDriver = availableDrivers.stream()
                .min(Comparator.comparingDouble(d -> calculateDistance(
                        request.getOriginLat(), request.getOriginLon(),
                        d.getCurrentLat(), d.getCurrentLon()
                )))
                .orElse(null);

        Trip trip = Trip.builder()
                .passenger(passenger)
                .driver(nearestDriver != null ? nearestDriver.getUser() : null)
                .originLat(request.getOriginLat())
                .originLon(request.getOriginLon())
                .originAddress(request.getOriginAddress())
                .destinationLat(request.getDestinationLat())
                .destinationLon(request.getDestinationLon())
                .destinationAddress(request.getDestinationAddress())
                .status(nearestDriver != null ? TripStatus.ACCEPTED : TripStatus.REQUESTED)
                .estimatedPrice(estimatedPrice)
                .acceptedAt(nearestDriver != null ? LocalDateTime.now() : null)
                .build();

        if (nearestDriver != null){
            nearestDriver.setAvailable(false);
            driverProfileRepository.save(nearestDriver);
        }

        Trip saved = tripRepository.save(trip);
        return toResponse(saved);
    }

    public TripResponse updateStatus(Long tripId, TripStatus newStatus, User user){
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        switch (newStatus){
            case IN_PROGRESS -> trip.setStartedAt(LocalDateTime.now());
            case COMPLETED -> {
                trip.setCompletedAt(LocalDateTime.now());
                trip.setFinalPrice(trip.getEstimatedPrice());
                driverProfileRepository.findByUserId(user.getId())
                        .ifPresent(d -> {
                            d.setAvailable(true);
                            driverProfileRepository.save(d);
                        });
            }
            case CANCELLED -> {
                trip.setCompletedAt(LocalDateTime.now());
                if (trip.getDriver() != null) {
                    driverProfileRepository.findByUserId(trip.getDriver().getId())
                            .ifPresent(d -> { d.setAvailable(true); driverProfileRepository.save(d); });
                }
            }
        }

        trip.setStatus(newStatus);
        return toResponse(tripRepository.save(trip));
    }

    public List<TripResponse> getMyTrips(User user) {
        if (user.getRole() == Role.PASSENGER) {
            return tripRepository.findByPassengerIdOrderByRequestedAtDesc(user.getId())
                    .stream().map(this::toResponse).toList();
        } else {
            return tripRepository.findByDriverIdOrderByRequestedAtDesc(user.getId())
                    .stream().map(this::toResponse).toList();
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng/2) * Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private TripResponse toResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .passengerName(trip.getPassenger().getName())
                .driverName(trip.getDriver() != null ? trip.getDriver().getName() : "Sin asignar")
                .originAddress(trip.getOriginAddress())
                .destinationAddress(trip.getDestinationAddress())
                .status(trip.getStatus())
                .estimatedPrice(trip.getEstimatedPrice())
                .requestedAt(trip.getRequestedAt())
                .build();
    }
}

