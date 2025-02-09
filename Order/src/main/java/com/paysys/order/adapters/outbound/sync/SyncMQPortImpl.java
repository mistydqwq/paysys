package com.paysys.order.adapters.outbound.sync;

import com.google.gson.Gson;
import com.paysys.order.infrastructure.RabbitMQConfig;
import com.paysys.order.ports.outbound.sync.SyncMQPort;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
