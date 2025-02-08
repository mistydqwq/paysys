package com.paysys.order.adapters.outbound.sync;

import com.paysys.order.domain.entities.Order;
import com.paysys.order.domain.enums.OrderStatusEnum;
import com.paysys.order.ports.outbound.OrderRepositoryPort;
import com.paysys.order.ports.outbound.sync.SyncMQPort;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepositoryImpl implements OrderRepositoryPort {

    private final RedissonClient redissonClient;
    private final SyncMQPort syncMQPort;
    private final OrderRepositoryPort dbRepository;
    private static final int ORDER_EXPIRE_AFTER_CREATED = 30;
    private static final long LOCK_WAIT_TIME = 10; // 锁等待时间（秒）
    private static final long LOCK_LEASE_TIME = 30; // 锁自动释放时间（秒）

    public RedisRepositoryImpl(RedissonClient redissonClient,
                               SyncMQPort syncMQPort,
                               @Qualifier("DBRepositoryImpl") OrderRepositoryPort dbRepository) {
        this.redissonClient = redissonClient;
        this.syncMQPort = syncMQPort;
        this.dbRepository = dbRepository;
    }

    private String getRedisKey(String orderId) {
        return "order:" + orderId;
    }

    private RLock getOrderLock(String orderId) {
        return redissonClient.getLock("order_lock:" + orderId);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        RLock lock = getOrderLock(orderId);
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<Object, Object> redisMap = redissonClient.getMap(getRedisKey(orderId));
                if (!redisMap.isEmpty()) {
                    return Optional.of(Order.fromRedisMap(redisMap));
                }

                Optional<Order> dbOrder = dbRepository.findById(orderId);
                dbOrder.ifPresent(order -> {
                    saveToRedisWithLock(order, 60, TimeUnit.MINUTES);
                });
                return dbOrder;
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
    public boolean save(Order order) {
        RLock lock = getOrderLock(order.getOrderId());
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                boolean redisResult = saveToRedisWithLock(order, getExpireTime(order), TimeUnit.MINUTES);
                if (redisResult) {
                    syncMQPort.publishMessage(getRedisKey(order.getOrderId()), "CREATE");
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
    public boolean update(Order order) {
        RLock lock = getOrderLock(order.getOrderId());
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                boolean redisResult = saveToRedisWithLock(order, getExpireTime(order), TimeUnit.MINUTES);
                if (redisResult) {
                    syncMQPort.publishMessage(getRedisKey(order.getOrderId()), "UPDATE");
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
    public boolean updateStatus(String orderId, int status) {
        RLock lock = getOrderLock(orderId);
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<Object, Object> redisMap = redissonClient.getMap(getRedisKey(orderId));

                if (redisMap.isEmpty()) {
                    Optional<Order> dbOrder = dbRepository.findById(orderId);
                    if (dbOrder.isEmpty()) return false;
                    saveToRedisWithLock(dbOrder.get(), getExpireTime(dbOrder.get()), TimeUnit.MINUTES);
                }

                redisMap.put("status", status);
                Order updatedOrder = Order.fromRedisMap(redisMap);
                updateExpireTimeWithLock(updatedOrder);
                syncMQPort.publishMessage(getRedisKey(orderId), "UPDATE");
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

    @Override
    public boolean updatePaymentLink(String orderId, String paymentLink) {
        RLock lock = getOrderLock(orderId);
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<Object, Object> redisMap = redissonClient.getMap(getRedisKey(orderId));

                if (redisMap.isEmpty()) {
                    Optional<Order> dbOrder = dbRepository.findById(orderId);
                    if (dbOrder.isEmpty()) return false;
                    saveToRedisWithLock(dbOrder.get(), getExpireTime(dbOrder.get()), TimeUnit.MINUTES);
                }

                redisMap.put("paymentLink", paymentLink);
                syncMQPort.publishMessage(getRedisKey(orderId), "UPDATE");
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

    @Override
    public boolean delete(String orderId) {
        RLock lock = getOrderLock(orderId);
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<Object, Object> redisMap = redissonClient.getMap(getRedisKey(orderId));
                boolean redisResult = redisMap.delete();
                if (redisResult) {
                    syncMQPort.publishMessage(getRedisKey(orderId), "DELETE");
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

    private boolean saveToRedisWithLock(Order order, long timeout, TimeUnit unit) {
        RLock lock = getOrderLock(order.getOrderId());
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<Object, Object> redisMap = redissonClient.getMap(getRedisKey(order.getOrderId()));
                redisMap.putAll(order.toRedisMap());
                redisMap.expire(timeout, unit);
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

    private long getExpireTime(Order order) {
        return order.getStatus() == OrderStatusEnum.CREATED ?
                ORDER_EXPIRE_AFTER_CREATED : 60;
    }

    private void updateExpireTimeWithLock(Order order) {
        RLock lock = getOrderLock(order.getOrderId());
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                RMap<Object, Object> redisMap = redissonClient.getMap(getRedisKey(order.getOrderId()));
                redisMap.expire(getExpireTime(order), TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}