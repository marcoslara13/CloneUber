package com.devmark.cloneuber.payment.dto;

import com.devmark.cloneuber.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long tripId;
    private Double amount;
    private PaymentStatus status;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
