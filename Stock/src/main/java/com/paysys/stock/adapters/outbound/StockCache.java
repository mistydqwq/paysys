package com.paysys.stock.adapters.outbound;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.domain.valueobj.StockVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StockCache {

    private static final String STOCK_CACHE_PREFIX = "stock:";
    private static final long CACHE_EXPIRE_MINUTES = 60; // 60分钟

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private RedissonClient redissonClient;

    // 获取库存缓存key
    private String getStockKey(String productId) {
        return STOCK_CACHE_PREFIX + productId;
    }

    // 从缓存获取库存
    public Optional<Stock> getFromCache(String productId) {
        RBucket<Stock> bucket = redissonClient.getBucket(getStockKey(productId));
        try {
            return Optional.ofNullable(bucket.get());
        } catch (Exception e) {
            log.error("Failed to get stock from cache", e);
            return Optional.empty();
        }
    }

    // 将库存写入缓存
    public void sendToCache(Stock stock) {
        RBucket<Stock> bucket = redissonClient.getBucket(getStockKey(stock.getProductId()));
        try {
            bucket.set(stock, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to send stock to cache", e);
        }
    }

    // 从缓存删除库存
    public void removeFromCache(String productId) {
        RBucket<Stock> bucket = redissonClient.getBucket(getStockKey(productId));
        try {
            bucket.delete();
        } catch (Exception e) {
            log.error("Failed to remove stock from cache", e);
        }
    }

    // 更新库存预留数量（原子操作）
    public boolean updateReservedQuantity(String productId, long quantity) {
        RMap<String, Long> stockMap = redissonClient.getMap(getStockKey(productId));
        try {
            return stockMap.computeIfPresent("reservedQuantity", (k, v) -> {
                long newValue = v + quantity;
                if (newValue < 0) return null; // 表示操作失败
                return newValue;
            }) != null;
        } catch (Exception e) {
            log.error("Failed to update reserved quantity in cache: {}", productId, e);
            return false;
        }
    }

    // 判断缓存是否存在
    public boolean exists(String productId) {
        RBucket<Stock> bucket = redissonClient.getBucket(getStockKey(productId));
        try {
            return bucket.isExists();
        } catch (Exception e) {
            log.error("Failed to check stock existence in cache: {}", productId, e);
            return false;
        }
    }

    // 获取缓存剩余时间（秒）
    public Long getExpireTime(String productId) {
        RBucket<Stock> bucket = redissonClient.getBucket(getStockKey(productId));
        try {
            return bucket.remainTimeToLive() / 1000; // 转换为秒
        } catch (Exception e) {
            log.error("Failed to get expire time of stock in cache: {}", productId, e);
            return -1L;
        }
    }

    // 缓存预热（使用 Redisson 批量操作）
    //@PostConstruct
    public void warmupCache() {
        log.info("Starting stock cache warming up...");
        try {
            QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_deleted", 0);
            List<StockVO> stocks = stockMapper.selectList(queryWrapper);

            stocks.parallelStream().forEach(stockVO -> {
                Stock stock = Stock.fromVO(stockVO);
                RBucket<Stock> bucket = redissonClient.getBucket(getStockKey(stock.getProductId()));
                bucket.set(stock, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            });

            log.info("Successfully warmed up {} stocks", stocks.size());
        } catch (Exception e) {
            log.error("Failed to warm up stock cache", e);
        }
    }

    // 定时缓存刷新（优化版）
    //@Scheduled(fixedRate = 300_000)
    public void refreshCache() {
        log.info("Starting stock cache refresh...");
        try {
            QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_deleted", 0);
            List<StockVO> stocks = stockMapper.selectList(queryWrapper);

            stocks.parallelStream().forEach(stockVO -> {
                Stock dbStock = Stock.fromVO(stockVO);
                String key = getStockKey(dbStock.getProductId());
                RBucket<Stock> bucket = redissonClient.getBucket(key);

                // 使用 compareAndSet 保证原子性更新
                bucket.compareAndSet(bucket.get(), dbStock);
            });

            log.info("Refreshed {} stocks", stocks.size());
        } catch (Exception e) {
            log.error("Failed to refresh stock cache", e);
        }
    }

    // 增强版预留库存操作（带分布式锁）
    public boolean safeUpdateReservedQuantity(String productId, long quantity) {
        RLock lock = redissonClient.getLock("stock_lock:" + productId);
        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                RBucket<Stock> bucket = redissonClient.getBucket(getStockKey(productId));
                Stock stock = bucket.get();

                if (stock != null) {
                    long newReserved = stock.getReservedQuantity() + quantity;
                    if (newReserved >= 0 && newReserved <= stock.getQuantity()) {
                        stock.setReservedQuantity(newReserved);
                        bucket.set(stock, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                        return true;
                    }
                }
                return false;
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