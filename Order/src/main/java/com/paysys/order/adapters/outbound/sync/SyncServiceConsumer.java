package com.paysys.order.adapters.outbound.sync;

import com.google.gson.Gson;
import com.paysys.order.adapters.outbound.OrderMapper;
import com.paysys.order.domain.entities.Order;
import com.paysys.order.infrastructure.RabbitMQConfig;
import com.paysys.order.ports.outbound.OrderRepositoryPort;
import com.paysys.order.ports.outbound.sync.SyncServicePort;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SyncServiceConsumer implements SyncServicePort {
    private final RedissonClient redissonClient;
    private final OrderRepositoryPort dbRepository;
    private final Gson gson;

    public SyncServiceConsumer(RedissonClient redissonClient,
                               @Qualifier("DBRepositoryImpl") OrderRepositoryPort dbRepository) {
        this.redissonClient = redissonClient;
        this.dbRepository = dbRepository;
        this.gson = new Gson();
    }

    @RabbitListener(queues = RabbitMQConfig.Sync_Queue)
    public void processSyncMessage(String message) {
        DataSyncEvent event = gson.fromJson(message, DataSyncEvent.class);
        synctoDB(event.getKey(), message);
    }

    @Override
    public boolean synctoDB(String redisKey, String message) {
        try {
            DataSyncEvent event = gson.fromJson(message, DataSyncEvent.class);
            String[] parts = event.getKey().split(":");
            String dataType = parts[0];
            String id = parts[1];

            switch (dataType) {
                case "order":
                    return handleOrderSync(event.getOperation(), id);
                default:
                    return false;
            }
        } catch (Exception e) {
            // 添加重试逻辑和日志
            System.err.println("Failed to process sync message: " + e.getMessage());
            return false;
        }
    }

    private boolean handleOrderSync(String operation, String orderId) {
        RMap<Object, Object> orderMap = redissonClient.getMap("order:" + orderId);

        try {
            Order order = convertToOrder(orderMap);
            return switch (operation.toUpperCase()) {
                case "CREATE", "UPDATE" -> saveOrUpdateOrder(order);
                case "DELETE" -> deleteOrder(orderId);
                default -> false;
            };
        } catch (DataConversionException e) {
            // 处理数据转换异常
            System.err.println("Failed to convert Redis data to Order: " + e.getMessage());
            return false;
        }
    }

    private Order convertToOrder(RMap<Object, Object> map) throws DataConversionException {
        try {
            return Order.fromRedisMap(map);
        } catch (Exception e) {
            throw new DataConversionException("Redis数据转换Order失败", e);
        }
    }

    private boolean saveOrUpdateOrder(Order order) {
        // 使用repository的保存逻辑
        Optional<Order> existing = dbRepository.findById(order.getOrderId());
        if (existing.isPresent()) {
            return dbRepository.update(order);
        }
        return dbRepository.save(order);
    }

    private boolean deleteOrder(String orderId) {
        return dbRepository.delete(orderId);
    }

    private static class DataConversionException extends Exception {
        public DataConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}