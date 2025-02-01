package com.paysys.payment.ports.inbound;

import com.paysys.payment.application.commands.UpdatePaymentStatusCommand;

public interface UpdatePaymentStatusUseCase {
    boolean updatePaymentStatus(UpdatePaymentStatusCommand command);
}
