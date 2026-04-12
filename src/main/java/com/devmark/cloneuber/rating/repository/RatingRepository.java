package com.devmark.cloneuber.rating.repository;

import com.devmark.cloneuber.rating.entity.Rating;
import com.devmark.cloneuber.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByRatedTo(User user);
    Boolean existsByTripId(Long tripId);
}
