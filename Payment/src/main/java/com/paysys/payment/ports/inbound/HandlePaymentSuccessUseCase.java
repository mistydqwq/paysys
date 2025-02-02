package com.paysys.payment.ports.inbound;

import com.paysys.payment.application.commands.PaymentResultCommand;

public interface HandlePaymentSuccessUseCase {
    Boolean handle(PaymentResultCommand command);
}
