package com.paysys.stock.adapters.outbound.sync;


import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import com.paysys.stock.ports.outbound.sync.SyncMQPort;
import com.paysys.vo.OrderItem;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepositoryImpl implements StockRepositoryPort {

    private final RedissonClient redissonClient;
    private final SyncMQPort syncMQPort;
    private final StockRepositoryPort dbRepository;


    private static final long LOCK_WAIT_TIME = 10;  // 锁等待时间（秒）
    private static final long LOCK_LEASE_TIME = 30; // 锁自动释放时间（秒）

    public RedisRepositoryImpl(RedissonClient redissonClient,
                               SyncMQPort syncMQPort,
                               @Qualifier("stockRepositoryImpl") StockRepositoryPort dbRepository) {
        this.redissonClient = redissonClient;
        this.syncMQPort = syncMQPort;
        this.dbRepository = dbRepository;
    }

    private String getRedisKey(String productId) {
        return "stock:" + productId;
    }

    private RLock getStockLock(String productId) {
        return redissonClient.getLock("stock_lock:" + productId);
    }

    @Override
    public Optional<Stock> findById(String productId) {
        RLock lock = getStockLock(productId);
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<Object, Object> redisMap = redissonClient.getMap(getRedisKey(productId));
                if (!redisMap.isEmpty()) {
                    return Optional.of(Stock.fromRedisMap(redisMap));
                }
                // Redis 中没有，从 DB 中查，并将结果存回 Redis
                Optional<Stock> dbStock = dbRepository.findById(productId);
                dbStock.ifPresent(this::saveToRedisWithLock);
                return dbStock;
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean save(Stock stock) {
        RLock lock = getStockLock(stock.getProductId());
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                boolean redisResult = saveToRedisWithLock(stock);
                if (redisResult) {
                    // 通知 MQ
                    syncMQPort.publishMessage(getRedisKey(stock.getProductId()), "CREATE");
                }
                return redisResult;
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean update(Stock stock) {
        RLock lock = getStockLock(stock.getProductId());
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                boolean redisResult = saveToRedisWithLock(stock);
                if (redisResult) {
                    // 通知 MQ
                    syncMQPort.publishMessage(getRedisKey(stock.getProductId()), "UPDATE");
                }
                return redisResult;
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean checkStockTransaction(String orderId, String type, String status) {
        RLock lock = getStockLock("tx:" + orderId);
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<String, String> redisMap = redissonClient.getMap("stockTx:" + orderId);
                return "true".equals(redisMap.get(type + ":" + status));
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public boolean reserveStock(String orderId, List<OrderItem> list) {
        if (orderId == null || orderId.isEmpty() || list == null || list.isEmpty()) {
            return false;
        }
        List<String> productIds = list.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .sorted()
                .toList();
        try {
            for (String productId: productIds) {
                RLock lock = getStockLock(productId);
                if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                    return false;
                }
            }
            for (OrderItem item: list) {
                RMap<String, Object> redisMap = redissonClient.getMap(getRedisKey(item.getProductId()));
                if (redisMap.isEmpty()) {
                    Optional<Stock> dbStock = dbRepository.findById(item.getProductId());
                    dbStock.ifPresent(this::saveToRedisWithLock);
                }
                int reservedQuantity = (int) redisMap.getOrDefault("reservedQuantity", 0);
                int quantity = (int) redisMap.getOrDefault("quantity", 0);
                if (quantity - reservedQuantity < item.getQuantity()) {
                    return false;
                }
                redisMap.put("reservedQuantity", reservedQuantity + item.getQuantity());
            }
            RMap<String, String> txMap = redissonClient.getMap("stockTx:" + orderId);
            txMap.put("RESERVE", "true");
            syncMQPort.publishMessage("stockTx:" + orderId, "RESERVE");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            productIds.forEach(pid -> {
                RLock lock = getStockLock(pid);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            });
        }
    }

    @Override
    @Transactional
    public boolean releaseStock(String orderId, List<OrderItem> list) {
        if (orderId == null || orderId.isEmpty() || list == null || list.isEmpty()) {
            return false;
        }
        List<String> productIds = list.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .sorted()
                .toList();
        try {
            for (String productId: productIds) {
                RLock lock = getStockLock(productId);
                if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                    return false;
                }
            }
            for (OrderItem item: list) {
                RMap<String, Object> redisMap = redissonClient.getMap(getRedisKey(item.getProductId()));
                if (redisMap.isEmpty()) {
                    Optional<Stock> dbStock = dbRepository.findById(item.getProductId());
                    dbStock.ifPresent(this::saveToRedisWithLock);
                }
                int reservedQuantity = (int) redisMap.getOrDefault("reservedQuantity", 0);
                if (reservedQuantity < item.getQuantity()) {
                    return false;
                }
                redisMap.put("reservedQuantity", reservedQuantity - item.getQuantity());
            }
            RMap<String, String> txMap = redissonClient.getMap("stockTx:" + orderId);
            txMap.put("RELEASE", "true");
            syncMQPort.publishMessage("stockTx:" + orderId, "RELEASE");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            productIds.forEach(pid -> {
                RLock lock = getStockLock(pid);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            });
        }
    }

    @Override
    public boolean delete(String productId) {
        RLock lock = getStockLock(productId);
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<Object, Object> redisMap = redissonClient.getMap(getRedisKey(productId));
                boolean redisResult = redisMap.delete();
                if (redisResult) {
                    // 通知 MQ
                    syncMQPort.publishMessage(getRedisKey(productId), "DELETE");
                }
                return redisResult;
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private boolean saveToRedisWithLock(Stock stock) {
        RLock lock = getStockLock(stock.getProductId());
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<Object, Object> redisMap = redissonClient.getMap(getRedisKey(stock.getProductId()));
                redisMap.putAll(stock.toRedisMap());
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}