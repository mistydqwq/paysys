package com.paysys.payment.application.commands;

import com.paysys.payment.domain.entities.Payment;
import com.paysys.payment.domain.enums.PaymentStatusEnum;
import com.paysys.payment.ports.inbound.UpdatePaymentStatusUseCase;
import com.paysys.payment.ports.outbound.PaymentRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class UpdatePaymentStatusHandler implements UpdatePaymentStatusUseCase {

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;


    @Override
    public boolean updatePaymentStatus(UpdatePaymentStatusCommand command) {
        Optional<Payment> paymentOptional = paymentRepositoryPort.findById(command.getTransactionId());
        if (paymentOptional.isEmpty()) {
            return false;
        }

        Payment payment = paymentOptional.get();
        PaymentStatusEnum newStatus = command.getTransactionStatus();
        if (newStatus == null) {
            return false;
        }

        payment.updateStatus(newStatus, command.getErrorCode(), command.getErrorMsg());
        payment.setChannelTransactionId(command.getChannelTransactionId());
        paymentRepositoryPort.save(payment);
        return true;
    }
}