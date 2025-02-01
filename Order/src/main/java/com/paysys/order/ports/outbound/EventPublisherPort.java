package com.paysys.order.ports.outbound;

import com.paysys.order.domain.events.OrderCreateEvent;

public interface EventPublisherPort {
    boolean publishEvent(OrderCreateEvent orderCreateEvent);
}
