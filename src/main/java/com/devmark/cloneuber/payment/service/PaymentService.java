package com.devmark.cloneuber.payment.service;

import com.devmark.cloneuber.payment.dto.PaymentRequest;
import com.devmark.cloneuber.payment.dto.PaymentResponse;
import com.devmark.cloneuber.payment.entity.Payment;
import com.devmark.cloneuber.payment.entity.PaymentStatus;
import com.devmark.cloneuber.payment.repository.PaymentRepository;
import com.devmark.cloneuber.trip.entity.Trip;
import com.devmark.cloneuber.trip.entity.TripStatus;
import com.devmark.cloneuber.trip.repository.TripRepository;
import com.devmark.cloneuber.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TripRepository tripRepository;

    public PaymentResponse createPayment(PaymentRequest paymentRequest, User requestingUser){
        Trip trip = tripRepository.findById(paymentRequest.getTripId()).orElseThrow(() -> new RuntimeException("Trip not found"));
        if (trip.getStatus() != TripStatus.COMPLETED) throw new RuntimeException("Trip not completed");
        if (paymentRepository.existsByTripId(trip.getId())) throw new RuntimeException("Payment already made");
        if (!paymentRequest.getAmount().equals(trip.getFinalPrice())) throw new RuntimeException("Amount not equal to price");

        if (!trip.getPassenger().getId().equals(requestingUser.getId())) {
            throw new RuntimeException("No puedes pagar un viaje que no es tuyo");
        }

        Payment payment = Payment.builder()
                .trip(trip)
                .amount(paymentRequest.getAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod(paymentRequest.getPaymentMethod())
                .createdAt(LocalDateTime.now())
                .paidAt(null)
                .build();
        paymentRepository.save(payment);
        return toResponse(payment);
    }

    public PaymentResponse processPayment(Long paymentId, User requestingUser){
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new RuntimeException("Payment not found"));
        Trip trip = payment.getTrip();
        if (payment.getStatus() != PaymentStatus.PENDING)
            throw new RuntimeException("Payment already processed");

        if (!trip.getPassenger().getId().equals(requestingUser.getId())) {
            throw new RuntimeException("No puedes pagar un viaje que no es tuyo");
        }

        Random random = new Random();

        Integer num = random.nextInt(100);

        if (num <= 10){
            payment.setStatus(PaymentStatus.FAILED);
        }else{
            payment.setStatus(PaymentStatus.COMPLETED);
        }

        payment.setPaidAt(LocalDateTime.now());

        paymentRepository.save(payment);
        return toResponse(payment);

    }

    public PaymentResponse getPayment(Long tripId, User requestingUser){
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new RuntimeException("Trip not found"));
        if (!trip.getPassenger().getId().equals(requestingUser.getId())) {
            throw new RuntimeException("Can't see a payment that is not yours");
        }
        Payment payment = paymentRepository.findByTripId(tripId);
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment){
        PaymentResponse paymentResponse = PaymentResponse.builder()
                .paymentId(payment.getId())
                .tripId(payment.getTrip().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();

        return paymentResponse;
    }
}
