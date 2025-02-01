package com.paysys.payment.ports.inbound;

import com.paysys.payment.application.commands.PaymentCommand;

public interface HandlePaymentSuccessUseCase {
    Boolean handle(PaymentCommand command);
}
