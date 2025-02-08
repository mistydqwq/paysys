package com.paysys.stock.adapters.outbound;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.domain.valueobj.StockVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StockCache {

    private static final String STOCK_CACHE_PREFIX = "stock:";
    private static final long CACHE_EXPIRE_TIME = 60; // 60分钟

    @Autowired
    private StockMapper stockMapper;

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

    // 缓存预热
//    @PostConstruct
    public void warmupCache() {
        log.info("Starting stock cache warming up...");
        try {
            // 1. 从数据库查询所有有效库存
            QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_deleted", 0);
            List<StockVO> stocks = stockMapper.selectList(queryWrapper);

            if (stocks.isEmpty()) {
                log.info("No stocks found for warming up");
                return;
            }

            // 2. 批量写入Redis
            for (StockVO stockVO : stocks) {
                Stock stock = Stock.fromVO(stockVO);
                String key = getStockKey(stock.getProductId());
                redisTemplate.opsForValue().set(
                        key,
                        stock,
                        CACHE_EXPIRE_TIME,
                        TimeUnit.MINUTES
                );
            }

            log.info("Successfully warmed up {} stocks to cache", stocks.size());
        } catch (Exception e) {
            log.error("Failed to warm up stock cache", e);
        }
    }

    // 定时刷新缓存
//    @Scheduled(fixedRate = 300000) // 5分钟刷新一次
    public void refreshCache() {
        log.info("Starting stock cache refresh...");
        try {
            // 1. 从数据库获取所有有效库存
            QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_deleted", 0);
            List<StockVO> stocks = stockMapper.selectList(queryWrapper);

            // 2. 遍历每个库存记录
            for (StockVO stockVO : stocks) {
                Stock stock = Stock.fromVO(stockVO);
                String key = getStockKey(stock.getProductId());

                // 3. 获取当前缓存中的库存
                Stock cachedStock = redisTemplate.opsForValue().get(key);

                // 4. 如果缓存不存在或者数据不一致，则更新缓存
                if (!stock.equals(cachedStock)) {
                    redisTemplate.opsForValue().set(
                            key,
                            stock,
                            CACHE_EXPIRE_TIME,
                            TimeUnit.MINUTES
                    );
                    log.debug("Updated cache for stock: {}", stock.getProductId());
                }
            }

            log.info("Successfully refreshed {} stocks in cache", stocks.size());
        } catch (Exception e) {
            log.error("Failed to refresh stock cache", e);
        }
    }

    // 手动触发缓存刷新
    public void forceRefresh() {
        log.info("Force refreshing stock cache...");
        refreshCache();
    }
}
