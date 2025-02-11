package com.paysys.stock.adapters.outbound.sync;


import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import com.paysys.stock.ports.outbound.sync.SyncMQPort;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

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