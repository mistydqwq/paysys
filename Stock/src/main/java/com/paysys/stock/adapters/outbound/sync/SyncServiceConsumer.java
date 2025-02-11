package com.paysys.stock.adapters.outbound.sync;

import com.google.gson.Gson;
import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.infrastructure.RabbitMQConfig;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import com.paysys.stock.ports.outbound.sync.SyncServicePort;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SyncServiceConsumer implements SyncServicePort {

    private final RedissonClient redissonClient;
    private final StockRepositoryPort dbRepository;
    private final Gson gson;

    public SyncServiceConsumer(RedissonClient redissonClient,
                               @Qualifier("dbRepositoryImpl") StockRepositoryPort dbRepository) {
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
                case "stock":
                    return handleStockSync(event.getOperation(), id);
                // 如果有其他类型的数据需要同步，可以继续在此处添加 case
                default:
                    return false;
            }
        } catch (Exception e) {
            // 可以添加重试逻辑和更详细的日志
            System.err.println("Failed to process sync message: " + e.getMessage());
            return false;
        }
    }

    private boolean handleStockSync(String operation, String productId) {
        // 统一使用 RMap<String, Object>
        RMap<String, Object> stockMap = redissonClient.getMap("stock:" + productId);

        try {
            // 将 Redis 中的哈希表数据转换为 Stock 对象
            Stock stock = convertToStock(stockMap);
            return switch (operation.toUpperCase()) {
                case "CREATE", "UPDATE" -> saveOrUpdateStock(stock);
                case "DELETE" -> deleteStock(productId);
                default -> false;
            };
        } catch (DataConversionException e) {
            // 处理数据转换异常
            System.err.println("Failed to convert Redis data to Stock: " + e.getMessage());
            return false;
        }
    }

    /**
     * 将 RMap<String, Object> 转换为 Stock 对象
     */
    private Stock convertToStock(RMap<String, Object> map) throws DataConversionException {
        try {
            return Stock.fromRedisMap(map);
        } catch (Exception e) {
            throw new DataConversionException("Redis 数据转换 Stock 失败", e);
        }
    }

    /**
     * 新增或更新 Stock
     */
    private boolean saveOrUpdateStock(Stock stock) {
        Optional<Stock> existing = dbRepository.findById(stock.getProductId());
        if (existing.isPresent()) {
            return dbRepository.update(stock);
        } else {
            return dbRepository.save(stock);
        }
    }

    /**
     * 删除 Stock
     */
    private boolean deleteStock(String stockId) {
        return dbRepository.delete(stockId);
    }

    /**
     * 自定义数据转换异常
     */
    private static class DataConversionException extends Exception {
        public DataConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
