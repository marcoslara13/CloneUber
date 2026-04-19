package com.devmark.cloneuber.payment.controller;

import com.devmark.cloneuber.common.response.ApiResponse;
import com.devmark.cloneuber.payment.dto.PaymentRequest;
import com.devmark.cloneuber.payment.dto.PaymentResponse;
import com.devmark.cloneuber.payment.service.PaymentService;
import com.devmark.cloneuber.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal User paidBy
            ){
        return ResponseEntity.ok(ApiResponse.ok(paymentService.createPayment(request, paidBy)));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal User paidBy
    ){
        return ResponseEntity.ok(ApiResponse.ok(paymentService.processPayment(id, paidBy)));
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long tripId,
            @AuthenticationPrincipal User paidBy
    ){
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPayment(tripId, paidBy)));
    }

}
