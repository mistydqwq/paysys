package com.paysys.payment.ports.inbound;

import com.paysys.payment.application.commands.PaymentResultCommand;

public interface HandlePaymentFailedUseCase {
    Boolean handle(PaymentResultCommand command);
}
