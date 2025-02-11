package com.paysys.stock.adapters.outbound.sync;

import com.google.gson.Gson;
import com.paysys.stock.infrastructure.RabbitMQConfig;
import com.paysys.stock.ports.outbound.sync.SyncMQPort;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class SyncMQPortImpl implements SyncMQPort {

    private final RabbitTemplate rabbitTemplate;
    private final Gson gson;

    public SyncMQPortImpl(RabbitTemplate rabbitTemplate, Gson gson) {
        this.rabbitTemplate = rabbitTemplate;
        this.gson = gson;
    }

    @Override
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public boolean publishMessage(String redisKey, String operationType) {
        try {
            DataSyncEvent event = new DataSyncEvent(redisKey, operationType);
            String message = gson.toJson(event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.Sync_Exchange,
                    RabbitMQConfig.Sync_Routing_Key,
                    message,
                    msg -> {
                        msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return msg;
                    });

            return true;
        } catch (Exception e) {
            // 添加日志
            return false;
        }
    }

}
