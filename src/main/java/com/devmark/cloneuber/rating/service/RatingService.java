package com.devmark.cloneuber.rating.service;

import com.devmark.cloneuber.rating.dto.RatingRequest;
import com.devmark.cloneuber.rating.dto.RatingResponse;
import com.devmark.cloneuber.rating.entity.Rating;
import com.devmark.cloneuber.rating.repository.RatingRepository;
import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.trip.entity.TripStatus;
import com.devmark.cloneuber.trip.repository.TripRepository;
import com.devmark.cloneuber.user.entity.User;
import com.devmark.cloneuber.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public RatingResponse rateTrip(RatingRequest ratingRequest, User ratedBy){
        if (ratingRepository.existsByTripId(ratingRequest.getTripId())) throw new RuntimeException("Viaje ya ha sido valorado");
        Trip trip = tripRepository.findById(ratingRequest.getTripId()).orElseThrow(() -> new RuntimeException("Viaje no encontrado"));
        if (trip.getStatus() != TripStatus.COMPLETED) throw new RuntimeException("Viaje no ha finalizado");
        User ratedTo;
        if (ratedBy.getId().equals(trip.getPassenger().getId())) {
            ratedTo = trip.getDriver();
        } else {
            ratedTo = trip.getPassenger();
        }
        Rating rating = Rating.builder()
                .trip(trip)
                .ratedBy(ratedBy)
                .ratedTo(ratedTo)
                .score(ratingRequest.getScore())
                .comment(ratingRequest.getComment())
                .timestamp(LocalDateTime.now())
                .build();
        ratingRepository.save(rating);
        return toResponse(rating);
    }

    public Double getAverageScore (Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuario no existe"));
        List<Rating> ratingList = ratingRepository.findByRatedTo(user);
        Double average = ratingList.stream()
                .map(Rating::getScore)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        return average;
    }

    private RatingResponse toResponse(Rating rating){
        RatingResponse ratingResponse = RatingResponse.builder()
                .ratingId(rating.getId())
                .tripId(rating.getTrip().getId())
                .ratedByName(rating.getRatedBy().getName())
                .ratedToName(rating.getRatedTo().getName())
                .score(rating.getScore())
                .comment(rating.getComment())
                .timestamp(rating.getTimestamp())
                .build();
        return ratingResponse;
    }
}
