package com.paysys.stock.infrastructure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Sync exchange and queue for sending messages
    public static final String Sync_Exchange = "sync.exchange";
    public static final String Sync_Queue = "sync.queue";
    public static final String Sync_Routing_Key = "sync.routing.key";

    // Sync exchange and queue for sending messages
    @Bean
    public DirectExchange syncExchange() {
        return new DirectExchange(Sync_Exchange);
    }

    @Bean
    public Queue syncQueue() {
        return new Queue(Sync_Queue, true); // durable = true
    }

    @Bean
    public Binding syncBinding(Queue syncQueue, DirectExchange syncExchange) {
        return BindingBuilder.bind(syncQueue).to(syncExchange).with(Sync_Routing_Key);
    }
}

