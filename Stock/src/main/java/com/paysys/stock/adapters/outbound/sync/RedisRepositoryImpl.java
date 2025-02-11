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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepositoryImpl implements StockRepositoryPort {

    private final RedissonClient redissonClient;
    private final SyncMQPort syncMQPort;
    private final StockRepositoryPort dbRepository;

    // 锁等待时间（秒）
    private static final long LOCK_WAIT_TIME = 10;
    // 锁自动释放时间（秒）
    private static final long LOCK_LEASE_TIME = 30;

    public RedisRepositoryImpl(RedissonClient redissonClient,
                               SyncMQPort syncMQPort,
                               @Qualifier("dbRepositoryImpl") StockRepositoryPort dbRepository) {
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
                // 统一用 RMap<String, Object>
                RMap<String, Object> redisMap = redissonClient.getMap(getRedisKey(productId));
                if (!redisMap.isEmpty()) {
                    // Redis 有缓存，直接构建 Stock
                    return Optional.of(Stock.fromRedisMap(redisMap));
                }
                // Redis 没有，则去 DB 查，再缓存到 Redis
                Optional<Stock> dbStock = dbRepository.findById(productId);
                dbStock.ifPresent(this::saveToRedisWithLock);
                return dbStock;
            }
            // 锁没拿到就返回空，也可以改成其他处理逻辑
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
                LocalDateTime now = LocalDateTime.now();
                stock.setCreatedAt(now);
                stock.setUpdatedAt(now);
                boolean result = saveToRedisWithLock(stock);
                if (result) {
                    // 通知消息队列
                    syncMQPort.publishMessage(getRedisKey(stock.getProductId()), "CREATE");
                }
                return result;
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
                boolean result = saveToRedisWithLock(stock);
                if (result) {
                    // 通知消息队列
                    syncMQPort.publishMessage(getRedisKey(stock.getProductId()), "UPDATE");
                }
                return result;
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

    /**
     * 检查订单对应的库存事务是否已存在某种状态 (type:status)
     */
    @Override
    public boolean checkStockTransaction(String orderId, String type, String status) {
        // 这里锁的 key 也做区分 "tx:" + orderId
        RLock lock = redissonClient.getLock("stock_lock:tx:" + orderId);
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                // 事务表统一用 RMap<String, String> 存 "true"/"false" 或其他字符串
                RMap<String, String> txMap = redissonClient.getMap("stockTx:" + orderId);
                String value = txMap.get(type + ":" + status);
                return "true".equals(value);
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
        // 拿到所有要锁的 productId
        List<String> productIds = list.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        try {
            // 先循环把所有锁拿到（避免第一个拿到锁后，第二个拿不到出现死锁情况）
            for (String productId : productIds) {
                RLock lock = getStockLock(productId);
                if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                    return false;
                }
            }
            // 全部锁拿到后，依次扣减 reservedQuantity
            for (OrderItem item : list) {
                RMap<String, Object> redisMap = redissonClient.getMap(getRedisKey(item.getProductId()));
                if (redisMap.isEmpty()) {
                    // 如果 Redis 里没有该库存信息，就从数据库捞
                    Optional<Stock> dbStock = dbRepository.findById(item.getProductId());
                    dbStock.ifPresent(this::saveToRedisWithLock);
                }

                // 读取 quantity / reservedQuantity
                long quantity = parseLongOrDefault(redisMap.get("quantity"), 0L);
                long reservedQty = parseLongOrDefault(redisMap.get("reservedQuantity"), 0L);

                // 检查可用库存是否足够
                if (quantity - reservedQty < item.getQuantity()) {
                    return false;
                }
                // 预留库存 + item.getQuantity()
                long newReserved = reservedQty + item.getQuantity();
                redisMap.put("reservedQuantity", newReserved);
            }
            // 记录事务状态
            RMap<String, String> txMap = redissonClient.getMap("stockTx:" + orderId);
            txMap.put("RESERVE", "true");
            // 通知 MQ
            syncMQPort.publishMessage("stockTx:" + orderId, "RESERVE");

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // 释放所有锁
            productIds.forEach(productId -> {
                RLock lock = getStockLock(productId);
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
        // 同样先拿所有锁
        List<String> productIds = list.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        try {
            for (String productId : productIds) {
                RLock lock = getStockLock(productId);
                if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                    return false;
                }
            }
            // 依次释放reservedQuantity
            for (OrderItem item : list) {
                RMap<String, Object> redisMap = redissonClient.getMap(getRedisKey(item.getProductId()));
                if (redisMap.isEmpty()) {
                    Optional<Stock> dbStock = dbRepository.findById(item.getProductId());
                    dbStock.ifPresent(this::saveToRedisWithLock);
                }

                long reservedQty = parseLongOrDefault(redisMap.get("reservedQuantity"), 0L);
                if (reservedQty < item.getQuantity()) {
                    // 如果当前预留比要释放的还少，说明数据异常
                    return false;
                }
                long newReserved = reservedQty - item.getQuantity();
                redisMap.put("reservedQuantity", newReserved);
            }
            // 记录事务状态
            RMap<String, String> txMap = redissonClient.getMap("stockTx:" + orderId);
            txMap.put("RELEASE", "true");
            // 通知 MQ
            syncMQPort.publishMessage("stockTx:" + orderId, "RELEASE");

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            productIds.forEach(productId -> {
                RLock lock = getStockLock(productId);
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
                RMap<String, Object> redisMap = redissonClient.getMap(getRedisKey(productId));
                boolean result = redisMap.delete();
                if (result) {
                    // 通知消息队列
                    syncMQPort.publishMessage(getRedisKey(productId), "DELETE");
                }
                return result;
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

    /**
     * 在已经持有本商品锁的情况下，把Stock对象保存到Redis
     */
    private boolean saveToRedisWithLock(Stock stock) {
        // 不再单独再上锁，因为外层已经上锁了，这里只判断一下
        RLock lock = getStockLock(stock.getProductId());
        try {
            if (lock.isHeldByCurrentThread()) {
                RMap<String, Object> redisMap = redissonClient.getMap(getRedisKey(stock.getProductId()));
                redisMap.putAll(stock.toRedisMap());
                return true;
            }
            // 如果进来却没持有锁，也可以选择 tryLock 一次
            return false;
        } finally {
            // 不在这里解锁，因为外层可能还要做别的操作
        }
    }

    /**
     * 一个小的工具方法，把 Redis 里的 "quantity"/"reservedQuantity" 等从 Object 转成 long
     */
    private long parseLongOrDefault(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
