package com.devmark.cloneuber.payment.repository;

import com.devmark.cloneuber.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByTripId(Long tripId);
    Boolean existsByTripId(Long tripId);
}
