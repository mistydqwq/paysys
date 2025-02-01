package com.paysys.payment.ports.inbound;

import com.paysys.payment.application.commands.CreatePaymentCommand;

public interface CreatePaymentUseCase {
    Boolean createPayment(CreatePaymentCommand createPaymentCommand);
}
