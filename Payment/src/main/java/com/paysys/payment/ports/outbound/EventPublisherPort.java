package com.paysys.payment.ports.outbound;

import com.paysys.payment.domain.events.PaymentCreateEvent;

public interface EventPublisherPort {
    boolean publishEvent(PaymentCreateEvent paymentCreateEvent);
}
