package com.devmark.cloneuber.unit;

import com.devmark.cloneuber.payment.dto.PaymentRequest;
import com.devmark.cloneuber.payment.dto.PaymentResponse;
import com.devmark.cloneuber.payment.entity.Payment;
import com.devmark.cloneuber.payment.entity.PaymentStatus;
import com.devmark.cloneuber.payment.repository.PaymentRepository;
import com.devmark.cloneuber.payment.service.PaymentService;
import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.trip.entity.TripStatus;
import com.devmark.cloneuber.trip.repository.TripRepository;
import com.devmark.cloneuber.user.entity.Role;
import com.devmark.cloneuber.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService — Tests unitarios")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private TripRepository tripRepository;

    @InjectMocks private PaymentService paymentService;

    private User passenger;
    private User driver;
    private Trip completedTrip;

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
    }

    @Nested
    @DisplayName("createPayment()")
    class CreatePayment {

        @Test
        @DisplayName("Pago válido → se crea en estado PENDING")
        void createPayment_validRequest_createsPending() {
            PaymentRequest request = PaymentRequest.builder()
                    .tripId(10L).amount(12.0).paymentMethod("CARD").build();

            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));
            when(paymentRepository.existsByTripId(10L)).thenReturn(false);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            PaymentResponse response = paymentService.createPayment(request, passenger);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(response.getAmount()).isEqualTo(12.0);
            assertThat(response.getPaymentMethod()).isEqualTo("CARD");
        }

        @Test
        @DisplayName("Viaje no completado → lanza RuntimeException")
        void createPayment_tripNotCompleted_throwsException() {
            completedTrip.setStatus(TripStatus.IN_PROGRESS);
            PaymentRequest request = PaymentRequest.builder()
                    .tripId(10L).amount(12.0).paymentMethod("CARD").build();

            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));

            assertThatThrownBy(() -> paymentService.createPayment(request, passenger))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Trip not completed");
        }

        @Test
        @DisplayName("Pago ya realizado → lanza RuntimeException")
        void createPayment_alreadyPaid_throwsException() {
            PaymentRequest request = PaymentRequest.builder()
                    .tripId(10L).amount(12.0).paymentMethod("CARD").build();

            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));
            when(paymentRepository.existsByTripId(10L)).thenReturn(true);

            assertThatThrownBy(() -> paymentService.createPayment(request, passenger))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Payment already made");
        }

        @Test
        @DisplayName("Importe incorrecto → lanza RuntimeException")
        void createPayment_wrongAmount_throwsException() {
            PaymentRequest request = PaymentRequest.builder()
                    .tripId(10L).amount(99.0).paymentMethod("CARD").build(); // precio real es 12.0

            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));
            when(paymentRepository.existsByTripId(10L)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.createPayment(request, passenger))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Amount not equal to price");
        }

        @Test
        @DisplayName("Pasajero incorrecto intenta pagar → lanza RuntimeException")
        void createPayment_wrongPassenger_throwsException() {
            User otherUser = User.builder().id(99L).name("Intruso").role(Role.PASSENGER).build();
            PaymentRequest request = PaymentRequest.builder()
                    .tripId(10L).amount(12.0).paymentMethod("CARD").build();

            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));
            when(paymentRepository.existsByTripId(10L)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.createPayment(request, otherUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no es tuyo");
        }
    }

    @Nested
    @DisplayName("processPayment()")
    class ProcessPayment {

        private Payment pendingPayment;

        @BeforeEach
        void setUpPayment() {
            pendingPayment = Payment.builder()
                    .id(1L).trip(completedTrip)
                    .amount(12.0).status(PaymentStatus.PENDING)
                    .paymentMethod("CARD").createdAt(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("Pago PENDING → se procesa y asigna paidAt")
        void processPayment_pending_setsResult() {
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.processPayment(1L, passenger);

            assertThat(response.getStatus()).isIn(PaymentStatus.COMPLETED, PaymentStatus.FAILED);
            assertThat(pendingPayment.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("Pago ya procesado → lanza RuntimeException")
        void processPayment_alreadyProcessed_throwsException() {
            pendingPayment.setStatus(PaymentStatus.COMPLETED);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

            assertThatThrownBy(() -> paymentService.processPayment(1L, passenger))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already processed");
        }

        @Test
        @DisplayName("Pasajero incorrecto → lanza RuntimeException")
        void processPayment_wrongUser_throwsException() {
            User intruder = User.builder().id(99L).name("Intruso").build();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

            assertThatThrownBy(() -> paymentService.processPayment(1L, intruder))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no es tuyo");
        }

        // El sistema tiene 10% de probabilidad de FAILED y 90% de COMPLETED.
        // Con 50 repeticiones verificamos que al menos ocurre un COMPLETED.
        @RepeatedTest(50)
        @DisplayName("Resultado aleatorio → siempre es COMPLETED o FAILED (nunca otro estado)")
        void processPayment_randomResult_isAlwaysValidStatus() {
            pendingPayment.setStatus(PaymentStatus.PENDING);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.processPayment(1L, passenger);

            assertThat(response.getStatus())
                    .isIn(PaymentStatus.COMPLETED, PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("getPayment()")
    class GetPayment {

        @Test
        @DisplayName("Pasajero correcto → devuelve el pago")
        void getPayment_correctPassenger_returnsPayment() {
            Payment payment = Payment.builder()
                    .id(1L).trip(completedTrip).amount(12.0)
                    .status(PaymentStatus.COMPLETED).paymentMethod("CARD")
                    .createdAt(LocalDateTime.now()).paidAt(LocalDateTime.now())
                    .build();

            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));
            when(paymentRepository.findByTripId(10L)).thenReturn(payment);

            PaymentResponse response = paymentService.getPayment(10L, passenger);

            assertThat(response.getAmount()).isEqualTo(12.0);
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Pasajero incorrecto → lanza RuntimeException")
        void getPayment_wrongPassenger_throwsException() {
            User intruder = User.builder().id(99L).name("Intruso").build();
            when(tripRepository.findById(10L)).thenReturn(Optional.of(completedTrip));

            assertThatThrownBy(() -> paymentService.getPayment(10L, intruder))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not yours");
        }
    }
}
