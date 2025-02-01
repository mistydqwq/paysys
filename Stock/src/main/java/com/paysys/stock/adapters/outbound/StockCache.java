package com.paysys.stock.adapters.outbound;

import com.paysys.stock.domain.entities.Stock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StockCache {

    private static final String STOCK_CACHE_PREFIX = "stock:";
    private static final long CACHE_EXPIRE_TIME = 60; // 60分钟

    @Autowired
    private RedisTemplate<String, Stock> redisTemplate;

    // 获取库存缓存key
    private String getStockKey(String productId) {
        return STOCK_CACHE_PREFIX + productId;
    }

    // 从缓存获取库存
    public Optional<Stock> getFromCache(String productId) {
        try {
            Stock stock = redisTemplate.opsForValue().get(getStockKey(productId));
            return Optional.ofNullable(stock);
        } catch (Exception e) {
            log.error("Failed to get stock from cache", e);
            return Optional.empty();
        }
    }

    // 将库存写入缓存
    public void sendToCache(Stock stock) {
        try {
            String key = getStockKey(stock.getProductId());
            redisTemplate.opsForValue().set(key, stock, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to send stock to cache", e);
        }
    }

    // 从缓存删除库存
    public void removeFromCache(String productId) {
        try {
            redisTemplate.delete(getStockKey(productId));
        } catch (Exception e) {
            log.error("Failed to remove stock from cache", e);
        }
    }

    // 更新库存预留数量
    public boolean updateReservedQuantity(String productId, long quantity) {
        try {
            String key = getStockKey(productId);
            Stock stock = redisTemplate.opsForValue().get(key);
            if (stock != null) {
                long newReserved = stock.getReservedQuantity() + quantity;
                if (newReserved >= 0 && newReserved <= stock.getQuantity()) {
                    stock.setReservedQuantity(newReserved);
                    redisTemplate.opsForValue().set(key, stock, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to update reserved quantity in cache: {}", productId, e);
            return false;
        }
    }

    // 判断缓存是否存在
    public boolean exists(String productId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(getStockKey(productId)));
        } catch (Exception e) {
            log.error("Failed to check stock existence in cache: {}", productId, e);
            return false;
        }
    }

    // 获取缓存过期时间
    public Long getExpireTime(String productId) {
        try {
            return redisTemplate.getExpire(getStockKey(productId));
        } catch (Exception e) {
            log.error("Failed to get expire time of stock in cache: {}", productId, e);
            return -1L;
        }
    }

    // 定时刷新缓存
    @Scheduled(fixedRate = 300000) // 5分钟
    public void refreshCache() {
        log.info("Refreshing stock cache...");
        // TODO: 实现定时刷新缓存的逻辑
        // 1. 从数据库获取所有库存

        // 2. 更新缓存
    }
}
