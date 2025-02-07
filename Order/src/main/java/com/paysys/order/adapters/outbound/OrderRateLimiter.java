package com.paysys.order.adapters.outbound;

import com.paysys.order.ports.outbound.RateLimiterPort;
import jakarta.annotation.Resource;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class OrderRateLimiter implements RateLimiterPort {
    @Resource
    private RedissonClient redissonClient;

    private static final String GLOBAL_LIMITER_KEY = "order:globalRateLimiter";

    private static final String CUSTOMER_LIMITER_PREFIX = "order:customerRateLimiter:";

    private final int globalRate = 100;

    private final int customerRate = 5;

    @Override
    public boolean tryAcquire(String customerId, int permits) {
        // 1.全局限流：获取全局 RateLimiter
        RRateLimiter globalRateLimiter = redissonClient.getRateLimiter(GLOBAL_LIMITER_KEY);
        if (!globalRateLimiter.isExists()) {
            // 如果全局限流器不存在，则创建，并设置速率：每秒 100 个令牌
            globalRateLimiter.trySetRate(RateType.OVERALL, globalRate, 1, RateIntervalUnit.SECONDS);
        }
        if (!globalRateLimiter.tryAcquire(permits)) {
            return false;
        }

        // 2.用户限流：获取当前用户的 RateLimiter
        String customerKey = CUSTOMER_LIMITER_PREFIX + customerId;
        RRateLimiter customerRateLimiter = redissonClient.getRateLimiter(customerKey);

        if (!customerRateLimiter.isExists()) {
            // 如果用户的限流器不存在，则创建，并设置速率：每秒 5 个令牌
            customerRateLimiter.trySetRate(RateType.OVERALL, customerRate, 1, RateIntervalUnit.SECONDS);
        }

        // 3.检查用户限流
        return customerRateLimiter.tryAcquire(permits);
    }
}
