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
class TripServiceTest {

    @Mock TripRepository tripRepository;
    @Mock DriverProfileRepository driverProfileRepository;

    @InjectMocks TripService tripService;

    private User passenger;
    private User driverUser;
    private DriverProfile driverProfile;
    private TripRequest tripRequest;

    @BeforeEach
    void setUp() {
        passenger = User.builder().id(1L).name("Pasajero").email("pas@test.com").role(Role.PASSENGER).build();
        driverUser = User.builder().id(2L).name("Conductor").email("con@test.com").role(Role.DRIVER).build();

        driverProfile = DriverProfile.builder()
                .id(1L)
                .user(driverUser)
                .available(true)
                .currentLat(-12.046374)
                .currentLon(-77.042793)
                .build();

        tripRequest = new TripRequest();
        tripRequest.setOriginLat(-12.046374);
        tripRequest.setOriginLon(-77.042793);
        tripRequest.setOriginAddress("Origen");
        tripRequest.setDestinationLat(-12.080);
        tripRequest.setDestinationLon(-77.080);
        tripRequest.setDestinationAddress("Destino");
    }

    // --- requestTrip ---

    @Test
    void requestTrip_conConductorDisponible_retornaViajeAceptado() {
        when(driverProfileRepository.findByAvailableTrue()).thenReturn(List.of(driverProfile));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TripResponse response = tripService.requestTrip(tripRequest, passenger);

        assertThat(response.getStatus()).isEqualTo(TripStatus.ACCEPTED);
        assertThat(response.getDriverName()).isEqualTo("Conductor");
        assertThat(response.getPassengerName()).isEqualTo("Pasajero");
        verify(driverProfileRepository).save(any(DriverProfile.class));
    }

    @Test
    void requestTrip_conConductorDisponible_marcaConductorNoDisponible() {
        when(driverProfileRepository.findByAvailableTrue()).thenReturn(List.of(driverProfile));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        tripService.requestTrip(tripRequest, passenger);

        assertThat(driverProfile.getAvailable()).isFalse();
        verify(driverProfileRepository).save(driverProfile);
    }

    @Test
    void requestTrip_sinConductoresDisponibles_retornaViajeEnEspera() {
        when(driverProfileRepository.findByAvailableTrue()).thenReturn(Collections.emptyList());
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });

        TripResponse response = tripService.requestTrip(tripRequest, passenger);

        assertThat(response.getStatus()).isEqualTo(TripStatus.REQUESTED);
        assertThat(response.getDriverName()).isEqualTo("Sin asignar");
        verify(driverProfileRepository, never()).save(any());
    }

    @Test
    void requestTrip_precioMinimoEsTres() {
        // Misma ubicación origen y destino → distancia ≈ 0 → precio mínimo 3.0
        tripRequest.setDestinationLat(tripRequest.getOriginLat());
        tripRequest.setDestinationLon(tripRequest.getOriginLon());

        when(driverProfileRepository.findByAvailableTrue()).thenReturn(Collections.emptyList());
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId(3L);
            return t;
        });

        TripResponse response = tripService.requestTrip(tripRequest, passenger);

        assertThat(response.getEstimatedPrice()).isGreaterThanOrEqualTo(3.0);
    }

    @Test
    void requestTrip_seleccionaConductorMasCercano() {
        DriverProfile lejano = DriverProfile.builder()
                .id(2L).user(User.builder().id(3L).name("Lejano").build())
                .available(true).currentLat(-13.0).currentLon(-78.0)
                .build();

        when(driverProfileRepository.findByAvailableTrue()).thenReturn(List.of(lejano, driverProfile));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId(4L);
            return t;
        });

        TripResponse response = tripService.requestTrip(tripRequest, passenger);

        // driverProfile está en las mismas coordenadas que el origen → más cercano
        assertThat(response.getDriverName()).isEqualTo("Conductor");
    }

    // --- updateStatus ---

    @Test
    void updateStatus_aInProgress_actualizaStartedAt() {
        Trip trip = Trip.builder()
                .id(1L).passenger(passenger).driver(driverUser)
                .status(TripStatus.ACCEPTED).estimatedPrice(10.0)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        TripResponse response = tripService.updateStatus(1L, TripStatus.IN_PROGRESS, driverUser);

        assertThat(response.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
        assertThat(trip.getStartedAt()).isNotNull();
    }

    @Test
    void updateStatus_aCompleted_estableceFinalPriceYLiberaConductor() {
        Trip trip = Trip.builder()
                .id(1L).passenger(passenger).driver(driverUser)
                .status(TripStatus.IN_PROGRESS).estimatedPrice(15.0)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(driverProfileRepository.findByUserId(driverUser.getId())).thenReturn(Optional.of(driverProfile));

        driverProfile.setAvailable(false);
        TripResponse response = tripService.updateStatus(1L, TripStatus.COMPLETED, driverUser);

        assertThat(response.getStatus()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.getFinalPrice()).isEqualTo(15.0);
        assertThat(trip.getCompletedAt()).isNotNull();
        assertThat(driverProfile.getAvailable()).isTrue();
        verify(driverProfileRepository).save(driverProfile);
    }

    @Test
    void updateStatus_aCancelled_liberaConductorSiExiste() {
        Trip trip = Trip.builder()
                .id(1L).passenger(passenger).driver(driverUser)
                .status(TripStatus.ACCEPTED).estimatedPrice(10.0)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(driverProfileRepository.findByUserId(driverUser.getId())).thenReturn(Optional.of(driverProfile));

        driverProfile.setAvailable(false);
        TripResponse response = tripService.updateStatus(1L, TripStatus.CANCELLED, driverUser);

        assertThat(response.getStatus()).isEqualTo(TripStatus.CANCELLED);
        assertThat(driverProfile.getAvailable()).isTrue();
    }

    @Test
    void updateStatus_aCancelled_sinDriver_noInteractaConDriverRepo() {
        Trip trip = Trip.builder()
                .id(1L).passenger(passenger).driver(null)
                .status(TripStatus.REQUESTED).estimatedPrice(10.0)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        tripService.updateStatus(1L, TripStatus.CANCELLED, passenger);

        verify(driverProfileRepository, never()).findByUserId(any());
    }

    @Test
    void updateStatus_viajeNoEncontrado_lanzaExcepcion() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.updateStatus(99L, TripStatus.IN_PROGRESS, driverUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Viaje no encontrado");
    }

    // --- getMyTrips ---

    @Test
    void getMyTrips_comoPasajero_consultaPorPassengerId() {
        Trip trip = Trip.builder()
                .id(1L).passenger(passenger).driver(driverUser)
                .status(TripStatus.COMPLETED).estimatedPrice(10.0)
                .build();
        when(tripRepository.findByPassengerIdOrderByRequestedAtDesc(1L)).thenReturn(List.of(trip));

        List<TripResponse> trips = tripService.getMyTrips(passenger);

        assertThat(trips).hasSize(1);
        assertThat(trips.get(0).getPassengerName()).isEqualTo("Pasajero");
        verify(tripRepository).findByPassengerIdOrderByRequestedAtDesc(1L);
        verify(tripRepository, never()).findByDriverIdOrderByRequestedAtDesc(any());
    }

    @Test
    void getMyTrips_comoConductor_consultaPorDriverId() {
        Trip trip = Trip.builder()
                .id(1L).passenger(passenger).driver(driverUser)
                .status(TripStatus.COMPLETED).estimatedPrice(10.0)
                .build();
        when(tripRepository.findByDriverIdOrderByRequestedAtDesc(2L)).thenReturn(List.of(trip));

        List<TripResponse> trips = tripService.getMyTrips(driverUser);

        assertThat(trips).hasSize(1);
        assertThat(trips.get(0).getDriverName()).isEqualTo("Conductor");
        verify(tripRepository).findByDriverIdOrderByRequestedAtDesc(2L);
        verify(tripRepository, never()).findByPassengerIdOrderByRequestedAtDesc(any());
    }

    @Test
    void getMyTrips_sinViajes_retornaListaVacia() {
        when(tripRepository.findByPassengerIdOrderByRequestedAtDesc(1L)).thenReturn(Collections.emptyList());

        List<TripResponse> trips = tripService.getMyTrips(passenger);

        assertThat(trips).isEmpty();
    }
}
