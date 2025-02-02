package com.paysys.payment.ports.inbound;

import com.paysys.payment.application.commands.PaymentResultCommand;

public interface HandlePaymentPendingUseCase {
    Boolean handle(PaymentResultCommand command);
}
