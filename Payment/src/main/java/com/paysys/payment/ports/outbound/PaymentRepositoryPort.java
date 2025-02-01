package com.paysys.payment.ports.outbound;

import com.paysys.payment.domain.entities.Payment;

import java.util.Optional;

public interface PaymentRepositoryPort {
    boolean save(Payment payment);
    Optional<Payment> findById(String transactionId);
    Optional<Payment> findByOrderId(String orderId);
    void deleteById(String transactionId);
    void update(Payment payment);
}
