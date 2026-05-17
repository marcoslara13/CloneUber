package com.devmark.cloneuber.unit;

import com.devmark.cloneuber.rating.dto.RatingRequest;
import com.devmark.cloneuber.rating.dto.RatingResponse;
import com.devmark.cloneuber.rating.entity.Rating;
import com.devmark.cloneuber.rating.repository.RatingRepository;
import com.devmark.cloneuber.rating.service.RatingService;
import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.trip.entity.TripStatus;
import com.devmark.cloneuber.trip.repository.TripRepository;
import com.devmark.cloneuber.user.entity.Role;
import com.devmark.cloneuber.user.entity.User;
import com.devmark.cloneuber.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RatingService — Tests unitarios")
class RatingServiceTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private TripRepository tripRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private RatingService ratingService;

    private User passenger;
    private User driver;
    private Trip completedTrip;
    private RatingRequest ratingRequest;

    @BeforeEach
    void setUp() {
        passenger = User.builder()
                .id(1L).name("Marcos").email("marcos@test.com")
                .role(Role.PASSENGER).build();

        driver = User.builder()
                .id(2L).name("Juan").email("juan@test.com")
                .role(Role.DRIVER).build();

        completedTrip = Trip.builder()
                .id(10L).passenger(passenger).driver(driver)
                .status(TripStatus.COMPLETED)
                .estimatedPrice(12.0).finalPrice(12.0)
                .completedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        ratingRequest = RatingRequest.builder()
                .tripId(10L).score(5).comment("Excelente conductor")
                .build();
    }

    @Nested
    @DisplayName("rateTrip()")
    class RateTrip {

        @Test
        @DisplayName("Pasajero valora al conductor → rating guardado correctamente")
        void rateTrip_passengerRatesDriver_savesRating() {
            when(ratingRepository.existsByTripId(10L)).thenReturn(false);
            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
                Rating r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            RatingResponse response = ratingService.rateTrip(ratingRequest, passenger);

            assertThat(response.getRatedByName()).isEqualTo("Marcos");
            assertThat(response.getRatedToName()).isEqualTo("Juan");
            assertThat(response.getScore()).isEqualTo(5);
            assertThat(response.getComment()).isEqualTo("Excelente conductor");
        }

        @Test
        @DisplayName("Conductor valora al pasajero → rating apunta al pasajero")
        void rateTrip_driverRatesPassenger_ratedToIsPassenger() {
            when(ratingRepository.existsByTripId(10L)).thenReturn(false);
            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));

            RatingResponse response = ratingService.rateTrip(ratingRequest, driver);

            assertThat(response.getRatedToName()).isEqualTo("Marcos");
        }

        @Test
        @DisplayName("Viaje ya valorado → lanza RuntimeException")
        void rateTrip_alreadyRated_throwsException() {
            when(ratingRepository.existsByTripId(10L)).thenReturn(true);

            assertThatThrownBy(() -> ratingService.rateTrip(ratingRequest, passenger))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ya ha sido valorado");

            verify(tripRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Viaje no completado → lanza RuntimeException")
        void rateTrip_tripNotCompleted_throwsException() {
            completedTrip.setStatus(TripStatus.IN_PROGRESS);
            when(ratingRepository.existsByTripId(10L)).thenReturn(false);
            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));

            assertThatThrownBy(() -> ratingService.rateTrip(ratingRequest, passenger))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no ha finalizado");
        }

        @Test
        @DisplayName("Viaje no encontrado → lanza RuntimeException")
        void rateTrip_tripNotFound_throwsException() {
            when(ratingRepository.existsByTripId(10L)).thenReturn(false);
            when(tripRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ratingService.rateTrip(ratingRequest, passenger))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Viaje no encontrado");
        }
    }

    @Nested
    @DisplayName("getAverageScore()")
    class GetAverageScore {

        @Test
        @DisplayName("Con ratings → devuelve la media correcta")
        void getAverageScore_withRatings_returnsCorrectAverage() {
            Rating r1 = Rating.builder().score(5).ratedTo(driver).build();
            Rating r2 = Rating.builder().score(3).ratedTo(driver).build();
            Rating r3 = Rating.builder().score(4).ratedTo(driver).build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(driver));
            when(ratingRepository.findByRatedTo(driver)).thenReturn(List.of(r1, r2, r3));

            Double average = ratingService.getAverageScore(2L);

            assertThat(average).isEqualTo(4.0); // (5+3+4)/3
        }

        @Test
        @DisplayName("Sin ratings → devuelve 0")
        void getAverageScore_noRatings_returnsZero() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(driver));
            when(ratingRepository.findByRatedTo(driver)).thenReturn(List.of());

            Double average = ratingService.getAverageScore(2L);

            assertThat(average).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Usuario no encontrado → lanza RuntimeException")
        void getAverageScore_userNotFound_throwsException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ratingService.getAverageScore(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuario no existe");
        }
    }
}
