package com.devmark.cloneuber.rating.service;

import com.devmark.cloneuber.rating.dto.RatingRequest;
import com.devmark.cloneuber.rating.dto.RatingResponse;
import com.devmark.cloneuber.rating.entity.Rating;
import com.devmark.cloneuber.rating.repository.RatingRepository;
import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.trip.entity.TripStatus;
import com.devmark.cloneuber.trip.repository.TripRepository;
import com.devmark.cloneuber.user.entity.Role;
import com.devmark.cloneuber.user.entity.User;
import com.devmark.cloneuber.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock RatingRepository ratingRepository;
    @Mock TripRepository tripRepository;
    @Mock UserRepository userRepository;

    @InjectMocks RatingService ratingService;

    private User passenger;
    private User driver;
    private Trip trip;
    private RatingRequest ratingRequest;

    @BeforeEach
    void setUp() {
        passenger = User.builder().id(1L).name("Pasajero").email("pasajero@test.com").role(Role.PASSENGER).build();
        driver = User.builder().id(2L).name("Conductor").email("conductor@test.com").role(Role.DRIVER).build();

        trip = Trip.builder()
                .id(10L)
                .passenger(passenger)
                .driver(driver)
                .status(TripStatus.COMPLETED)
                .build();

        ratingRequest = RatingRequest.builder()
                .tripId(10L)
                .score(5)
                .comment("Excelente")
                .build();
    }

    // --- rateTrip ---

    @Test
    void rateTrip_pasajeroCalificaConductor_exitoso() {
        when(ratingRepository.existsByTripId(10L)).thenReturn(false);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        RatingResponse response = ratingService.rateTrip(ratingRequest, passenger);

        assertThat(response.getTripId()).isEqualTo(10L);
        assertThat(response.getScore()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Excelente");
        assertThat(response.getRatedByName()).isEqualTo("Pasajero");
        assertThat(response.getRatedToName()).isEqualTo("Conductor");
    }

    @Test
    void rateTrip_conductorCalificaPasajero_exitoso() {
        when(ratingRepository.existsByTripId(10L)).thenReturn(false);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });

        RatingResponse response = ratingService.rateTrip(ratingRequest, driver);

        assertThat(response.getRatedByName()).isEqualTo("Conductor");
        assertThat(response.getRatedToName()).isEqualTo("Pasajero");
    }

    @Test
    void rateTrip_yaCalificado_lanzaExcepcion() {
        when(ratingRepository.existsByTripId(10L)).thenReturn(true);

        assertThatThrownBy(() -> ratingService.rateTrip(ratingRequest, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Viaje ya ha sido valorado");

        verify(tripRepository, never()).findById(any());
    }

    @Test
    void rateTrip_viajeNoEncontrado_lanzaExcepcion() {
        when(ratingRepository.existsByTripId(10L)).thenReturn(false);
        when(tripRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.rateTrip(ratingRequest, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Viaje no encontrado");
    }

    @Test
    void rateTrip_viajeNoCompletado_lanzaExcepcion() {
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(ratingRepository.existsByTripId(10L)).thenReturn(false);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> ratingService.rateTrip(ratingRequest, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Viaje no ha finalizado");
    }

    @Test
    void rateTrip_guardaRatingConTimestamp() {
        when(ratingRepository.existsByTripId(10L)).thenReturn(false);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        ratingService.rateTrip(ratingRequest, passenger);

        verify(ratingRepository).save(argThat(r -> r.getTimestamp() != null));
    }

    // --- getAverageScore ---

    @Test
    void getAverageScore_conVariasCalificaciones_retornaPromedio() {
        Rating r1 = Rating.builder().score(4).build();
        Rating r2 = Rating.builder().score(5).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(ratingRepository.findByRatedTo(passenger)).thenReturn(List.of(r1, r2));

        Double average = ratingService.getAverageScore(1L);

        assertThat(average).isEqualTo(4.5);
    }

    @Test
    void getAverageScore_unaCalificacion_retornaEsaCalificacion() {
        Rating r1 = Rating.builder().score(3).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(ratingRepository.findByRatedTo(passenger)).thenReturn(List.of(r1));

        assertThat(ratingService.getAverageScore(1L)).isEqualTo(3.0);
    }

    @Test
    void getAverageScore_sinCalificaciones_retornaCero() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(ratingRepository.findByRatedTo(passenger)).thenReturn(Collections.emptyList());

        assertThat(ratingService.getAverageScore(1L)).isEqualTo(0.0);
    }

    @Test
    void getAverageScore_usuarioNoExiste_lanzaExcepcion() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.getAverageScore(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no existe");
    }
}
