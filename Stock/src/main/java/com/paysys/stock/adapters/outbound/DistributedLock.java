package com.paysys.stock.adapters.outbound;

import com.paysys.stock.ports.outbound.DistributedLockPort;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DistributedLock implements DistributedLockPort {
    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "stock:lock:";
    private static final int MAX_RETRY_TIMES = 3;
    private static final long RETRY_DELAY_MS = 100;

    public DistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean acquireLock(String productId, long timeoutSeconds) {
        int retryTimes = 0;
        while (retryTimes < MAX_RETRY_TIMES) {
            try {
                RLock lock = redissonClient.getLock(LOCK_PREFIX + productId);
                // 添加看门狗，防止锁过期后，业务还未执行完毕，导致锁被释放
                if (lock.tryLock(timeoutSeconds, 30, TimeUnit.SECONDS)) {
                    return true;
                }
                retryTimes++;
                Thread.sleep(RETRY_DELAY_MS * retryTimes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                log.error("Failed to acquire lock for product: {}", productId, e);
                retryTimes++;
            }
        }
        return false;
    }

    @Override
    public void releaseLock(String productId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + productId);
        try {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("Failed to release lock for product: {}", productId, e);
        }
    }

    public boolean isLocked(String productId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + productId);
        return lock.isLocked();
    }
}
