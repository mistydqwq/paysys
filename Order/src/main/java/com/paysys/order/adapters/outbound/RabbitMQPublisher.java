package com.paysys.order.adapters.outbound;

import com.google.gson.Gson;
import com.paysys.order.domain.events.OrderCreateEvent;
import com.paysys.order.infrastructure.RabbitMQConfig;
import com.paysys.order.ports.outbound.EventPublisherPort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQPublisher implements EventPublisherPort {

    private final RabbitTemplate rabbitTemplate;
    private final Gson gson;

    public RabbitMQPublisher(RabbitTemplate rabbitTemplate, Gson gson) {
        this.rabbitTemplate = rabbitTemplate;
        this.gson = gson;
    }

    @Override
    public boolean publishEvent(OrderCreateEvent orderCreateEvent) {
        try {
            String jsonEvent = gson.toJson(orderCreateEvent);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_ROUTING_KEY,
                    jsonEvent
            );
            System.out.println("Event published: " + jsonEvent);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to publish event: " + e.getMessage());
            return false;
        }
    }
}
