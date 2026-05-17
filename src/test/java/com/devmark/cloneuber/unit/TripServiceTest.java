package com.devmark.cloneuber.unit;

import com.devmark.cloneuber.driver.entity.DriverProfile;
import com.devmark.cloneuber.driver.repository.DriverProfileRepository;
import com.devmark.cloneuber.trip.dto.TripRequest;
import com.devmark.cloneuber.trip.dto.TripResponse;
import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.trip.entity.TripStatus;
import com.devmark.cloneuber.trip.repository.TripRepository;
import com.devmark.cloneuber.trip.service.TripService;
import com.devmark.cloneuber.user.entity.Role;
import com.devmark.cloneuber.user.entity.User;
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
@DisplayName("TripService — Tests unitarios")
class TripServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private DriverProfileRepository driverProfileRepository;

    @InjectMocks private TripService tripService;

    private User passenger;
    private User driverUser;
    private DriverProfile driverProfile;
    private TripRequest tripRequest;

    @BeforeEach
    void setUp() {
        passenger = User.builder()
                .id(1L).name("Marcos").email("marcos@test.com")
                .password("pass").role(Role.PASSENGER).build();

        driverUser = User.builder()
                .id(2L).name("Juan").email("juan@test.com")
                .password("pass").role(Role.DRIVER).build();

        // Driver cerca del origen del viaje (Puerta del Sol)
        driverProfile = DriverProfile.builder()
                .id(1L).user(driverUser)
                .vehiclePlate("1234ABC").vehicleModel("Toyota Prius")
                .available(true)
                .currentLat(40.4168).currentLon(-3.7038) // Madrid centro
                .build();

        tripRequest = TripRequest.builder()
                .originLat(40.4170).originLon(-3.7040)
                .originAddress("Puerta del Sol, Madrid")
                .destinationLat(40.4530).destinationLon(-3.6883)
                .destinationAddress("Bernabéu, Madrid")
                .build();
    }

    // ─────────────────────────────────────────────
    // requestTrip
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("requestTrip()")
    class RequestTrip {

        @Test
        @DisplayName("Con conductor disponible → viaje en estado ACCEPTED y conductor asignado")
        void requestTrip_withAvailableDriver_returnsAccepted() {
            when(driverProfileRepository.findByAvailableTrue()).thenReturn(List.of(driverProfile));
            when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
                Trip t = inv.getArgument(0);
                t.setId(10L);
                return t;
            });

            TripResponse response = tripService.requestTrip(tripRequest, passenger);

            assertThat(response.getStatus()).isEqualTo(TripStatus.ACCEPTED);
            assertThat(response.getDriverName()).isEqualTo("Juan");
            assertThat(response.getPassengerName()).isEqualTo("Marcos");
            assertThat(response.getEstimatedPrice()).isGreaterThanOrEqualTo(3.0);

            // El conductor debe haberse marcado como no disponible
            verify(driverProfileRepository).save(argThat(d -> !d.getAvailable()));
        }

        @Test
        @DisplayName("Sin conductores disponibles → viaje en estado REQUESTED sin conductor")
        void requestTrip_noDriversAvailable_returnsRequested() {
            when(driverProfileRepository.findByAvailableTrue()).thenReturn(List.of());
            when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
                Trip t = inv.getArgument(0);
                t.setId(11L);
                return t;
            });

            TripResponse response = tripService.requestTrip(tripRequest, passenger);

            assertThat(response.getStatus()).isEqualTo(TripStatus.REQUESTED);
            assertThat(response.getDriverName()).isEqualTo("Sin asignar");

            // No se debe guardar ningún DriverProfile
            verify(driverProfileRepository, never()).save(any(DriverProfile.class));
        }

        @Test
        @DisplayName("Precio mínimo de 3€ aunque la distancia sea corta")
        void requestTrip_shortDistance_appliesMinimumPrice() {
            // Origen y destino casi en el mismo punto
            TripRequest shortTrip = TripRequest.builder()
                    .originLat(40.4168).originLon(-3.7038)
                    .originAddress("A").destinationLat(40.4169).destinationLon(-3.7039)
                    .destinationAddress("B").build();

            when(driverProfileRepository.findByAvailableTrue()).thenReturn(List.of());
            when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

            TripResponse response = tripService.requestTrip(shortTrip, passenger);

            assertThat(response.getEstimatedPrice()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Con varios conductores → se asigna el más cercano al origen")
        void requestTrip_multipleDrivers_assignsNearest() {
            // Driver 1: muy cerca del origen (Madrid centro)
            DriverProfile nearDriver = DriverProfile.builder()
                    .id(1L).user(driverUser).available(true)
                    .currentLat(40.4170).currentLon(-3.7041).build(); // ~0.01 km

            // Driver 2: lejos (Getafe)
            User farUser = User.builder().id(3L).name("Pedro").build();
            DriverProfile farDriver = DriverProfile.builder()
                    .id(2L).user(farUser).available(true)
                    .currentLat(40.3050).currentLon(-3.7230).build(); // ~12 km

            when(driverProfileRepository.findByAvailableTrue()).thenReturn(List.of(farDriver, nearDriver));
            when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

            TripResponse response = tripService.requestTrip(tripRequest, passenger);

            assertThat(response.getDriverName()).isEqualTo("Juan"); // el más cercano
        }
    }

    // ─────────────────────────────────────────────
    // updateStatus
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        private Trip existingTrip;

        @BeforeEach
        void setUpTrip() {
            existingTrip = Trip.builder()
                    .id(10L).passenger(passenger).driver(driverUser)
                    .originAddress("Puerta del Sol").destinationAddress("Bernabéu")
                    .status(TripStatus.ACCEPTED).estimatedPrice(8.0).build();
        }

        @Test
        @DisplayName("ACCEPTED → IN_PROGRESS asigna startedAt")
        void updateStatus_toInProgress_setsStartedAt() {
            when(tripRepository.findById(10L)).thenReturn(Optional.of(existingTrip));
            when(tripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TripResponse response = tripService.updateStatus(10L, TripStatus.IN_PROGRESS, driverUser);

            assertThat(response.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
            assertThat(existingTrip.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("IN_PROGRESS → COMPLETED asigna finalPrice y libera al conductor")
        void updateStatus_toCompleted_setsFinalPriceAndFreesDriver() {
            existingTrip.setStatus(TripStatus.IN_PROGRESS);
            existingTrip.setStartedAt(LocalDateTime.now().minusMinutes(10));

            when(tripRepository.findById(10L)).thenReturn(Optional.of(existingTrip));
            when(tripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(driverProfileRepository.findByUserId(driverUser.getId()))
                    .thenReturn(Optional.of(driverProfile));

            TripResponse response = tripService.updateStatus(10L, TripStatus.COMPLETED, driverUser);

            assertThat(response.getStatus()).isEqualTo(TripStatus.COMPLETED);
            assertThat(existingTrip.getFinalPrice()).isEqualTo(existingTrip.getEstimatedPrice());
            assertThat(existingTrip.getCompletedAt()).isNotNull();

            // El conductor queda disponible
            verify(driverProfileRepository).save(argThat(DriverProfile::getAvailable));
        }

        @Test
        @DisplayName("CANCELLED libera al conductor si tenía uno asignado")
        void updateStatus_cancelled_freesDriver() {
            when(tripRepository.findById(10L)).thenReturn(Optional.of(existingTrip));
            when(tripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(driverProfileRepository.findByUserId(driverUser.getId()))
                    .thenReturn(Optional.of(driverProfile));

            tripService.updateStatus(10L, TripStatus.CANCELLED, driverUser);

            verify(driverProfileRepository).save(argThat(DriverProfile::getAvailable));
        }

        @Test
        @DisplayName("Viaje no encontrado → lanza RuntimeException")
        void updateStatus_tripNotFound_throwsException() {
            when(tripRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.updateStatus(99L, TripStatus.IN_PROGRESS, driverUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Viaje no encontrado");
        }
    }

    // ─────────────────────────────────────────────
    // getMyTrips
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("getMyTrips()")
    class GetMyTrips {

        @Test
        @DisplayName("Pasajero → consulta por passengerId")
        void getMyTrips_asPassenger_queriesPassengerRepo() {
            Trip trip = Trip.builder()
                    .id(1L).passenger(passenger).driver(driverUser)
                    .status(TripStatus.COMPLETED).estimatedPrice(10.0)
                    .originAddress("A").destinationAddress("B").build();

            when(tripRepository.findByPassengerIdOrderByRequestedAtDesc(passenger.getId()))
                    .thenReturn(List.of(trip));

            List<TripResponse> result = tripService.getMyTrips(passenger);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPassengerName()).isEqualTo("Marcos");
            verify(tripRepository).findByPassengerIdOrderByRequestedAtDesc(passenger.getId());
            verify(tripRepository, never()).findByDriverIdOrderByRequestedAtDesc(any());
        }

        @Test
        @DisplayName("Conductor → consulta por driverId")
        void getMyTrips_asDriver_queriesDriverRepo() {
            Trip trip = Trip.builder()
                    .id(2L).passenger(passenger).driver(driverUser)
                    .status(TripStatus.COMPLETED).estimatedPrice(10.0)
                    .originAddress("A").destinationAddress("B").build();

            when(tripRepository.findByDriverIdOrderByRequestedAtDesc(driverUser.getId()))
                    .thenReturn(List.of(trip));

            List<TripResponse> result = tripService.getMyTrips(driverUser);

            assertThat(result).hasSize(1);
            verify(tripRepository).findByDriverIdOrderByRequestedAtDesc(driverUser.getId());
            verify(tripRepository, never()).findByPassengerIdOrderByRequestedAtDesc(any());
        }
    }
}
