package com.devmark.cloneuber.payment.service;

import com.devmark.cloneuber.payment.dto.PaymentRequest;
import com.devmark.cloneuber.payment.dto.PaymentResponse;
import com.devmark.cloneuber.payment.entity.Payment;
import com.devmark.cloneuber.payment.entity.PaymentStatus;
import com.devmark.cloneuber.payment.repository.PaymentRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock TripRepository tripRepository;

    @InjectMocks PaymentService paymentService;

    private User passenger;
    private Trip trip;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        passenger = User.builder()
                .id(1L).name("Juan").email("juan@test.com").role(Role.PASSENGER)
                .build();

        trip = Trip.builder()
                .id(10L)
                .passenger(passenger)
                .status(TripStatus.COMPLETED)
                .estimatedPrice(15.0)
                .finalPrice(15.0)
                .build();

        paymentRequest = PaymentRequest.builder()
                .tripId(10L)
                .amount(15.0)
                .paymentMethod("CARD")
                .build();
    }

    // --- createPayment ---

    @Test
    void createPayment_exitoso_retornaPaymentPendiente() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(paymentRepository.existsByTripId(10L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        PaymentResponse response = paymentService.createPayment(paymentRequest, passenger);

        assertThat(response.getTripId()).isEqualTo(10L);
        assertThat(response.getAmount()).isEqualTo(15.0);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentMethod()).isEqualTo("CARD");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_viajeNoEncontrado_lanzaExcepcion() {
        when(tripRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(paymentRequest, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Trip not found");
    }

    @Test
    void createPayment_viajeNoCompletado_lanzaExcepcion() {
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> paymentService.createPayment(paymentRequest, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Trip not completed");
    }

    @Test
    void createPayment_pagoYaExiste_lanzaExcepcion() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(paymentRepository.existsByTripId(10L)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.createPayment(paymentRequest, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Payment already made");
    }

    @Test
    void createPayment_montoIncorrecto_lanzaExcepcion() {
        paymentRequest.setAmount(20.0);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(paymentRepository.existsByTripId(10L)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.createPayment(paymentRequest, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Amount not equal to price");
    }

    @Test
    void createPayment_usuarioNoEsPasajero_lanzaExcepcion() {
        User otro = User.builder().id(99L).name("Otro").role(Role.DRIVER).build();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(paymentRepository.existsByTripId(10L)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.createPayment(paymentRequest, otro))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No puedes pagar un viaje que no es tuyo");
    }

    // --- processPayment ---

    @Test
    void processPayment_exitoso_retornaCompletadoOFallido() {
        Payment payment = Payment.builder()
                .id(100L).trip(trip).amount(15.0)
                .status(PaymentStatus.PENDING).paymentMethod("CARD")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.processPayment(100L, passenger);

        assertThat(response.getStatus()).isIn(PaymentStatus.COMPLETED, PaymentStatus.FAILED);
        assertThat(response.getPaidAt()).isNotNull();
    }

    @Test
    void processPayment_guardaPaidAt() {
        Payment payment = Payment.builder()
                .id(100L).trip(trip).amount(15.0)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(100L, passenger);

        verify(paymentRepository).save(argThat(p -> p.getPaidAt() != null));
    }

    @Test
    void processPayment_pagoNoEncontrado_lanzaExcepcion() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(999L, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Payment not found");
    }

    @Test
    void processPayment_pagoYaProcesado_lanzaExcepcion() {
        Payment payment = Payment.builder()
                .id(100L).trip(trip).status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processPayment(100L, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Payment already processed");
    }

    @Test
    void processPayment_usuarioNoEsPasajero_lanzaExcepcion() {
        User otro = User.builder().id(99L).name("Otro").role(Role.DRIVER).build();
        Payment payment = Payment.builder()
                .id(100L).trip(trip).status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processPayment(100L, otro))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No puedes pagar un viaje que no es tuyo");
    }

    // --- getPayment ---

    @Test
    void getPayment_exitoso_retornaPago() {
        Payment payment = Payment.builder()
                .id(100L).trip(trip).amount(15.0)
                .status(PaymentStatus.COMPLETED).paymentMethod("CARD")
                .createdAt(LocalDateTime.now()).paidAt(LocalDateTime.now())
                .build();

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(paymentRepository.findByTripId(10L)).thenReturn(payment);

        PaymentResponse response = paymentService.getPayment(10L, passenger);

        assertThat(response.getTripId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void getPayment_viajeNoEncontrado_lanzaExcepcion() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(99L, passenger))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Trip not found");
    }

    @Test
    void getPayment_usuarioNoEsPasajero_lanzaExcepcion() {
        User otro = User.builder().id(99L).build();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> paymentService.getPayment(10L, otro))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Can't see a payment that is not yours");
    }
}
