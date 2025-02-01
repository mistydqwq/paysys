package com.paysys.stock.adapters.outbound;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.domain.valueobj.OrderItem;
import com.paysys.stock.domain.valueobj.StockTransactionVO;
import com.paysys.stock.domain.valueobj.StockVO;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class StockRepositoryImpl implements StockRepositoryPort {

    private final StockMapper stockMapper;
    private final StockCache stockCache;
    private final StockTransactionMapper stockTransactionMapper;

    public StockRepositoryImpl(StockMapper stockMapper, StockCache stockCache, StockTransactionMapper stockTransactionMapper) {
        this.stockMapper = stockMapper;
        this.stockCache = stockCache;
        this.stockTransactionMapper = stockTransactionMapper;
    }

    @Override
    public Optional<Stock> findById(String productId) {
        if (productId == null || productId.isEmpty()) {
            return Optional.empty();
        }

        // 先从缓存获取
        Optional<Stock> cachedStock = stockCache.getFromCache(productId);
        if (cachedStock.isPresent()) {
            return cachedStock;
        }

        // 缓存未命中，从数据库获取
        QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        Optional<Stock> stock = Optional.ofNullable(stockMapper.selectOne(queryWrapper)).map(Stock::fromVO);

        // 将库存写入缓存
        stock.ifPresent(stockCache::sendToCache);

        return stock;
    }

    @Override
    @Transactional
    public boolean save(Stock stock) {
        if (stock == null) {
            return false;
        }

        try {
            // 保存到数据库
            boolean success = stockMapper.insert(stock.toVO()) == 1;

            // 更新缓存
            if (success) {
                stockCache.sendToCache(stock);
            }

            return success;
        } catch (Exception e) {
            log.error("Failed to save stock: {}", stock.getProductId(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean update(Stock stock) {
        if (stock == null) {
            return false;
        }

        try {
            // 更新数据库
            UpdateWrapper<StockVO> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("product_id", stock.getProductId());
            StockVO stockVO = stock.toVO();
            boolean success = stockMapper.update(stockVO, updateWrapper) == 1;

            // 更新缓存
            if (success) {
                stockCache.sendToCache(stock);
            } else {
                stockCache.removeFromCache(stock.getProductId());
            }

            return success;
        } catch (Exception e) {
            log.error("Failed to update stock: {}", stock.getProductId(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean reserveStock(String orderId, List<OrderItem> list) {
        if (orderId == null || orderId.isEmpty() || list == null || list.isEmpty()) {
            return false;
        }

        try {
            for (OrderItem orderItem : list) {
                // 先查询库存
                QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("product_id", orderItem.getProductId());
                StockVO stock = stockMapper.selectOne(queryWrapper);

                // 检查库存是否存在且足够
                if (stock == null || stock.getQuantity() < stock.getReservedQuantity() + orderItem.getQuantity()) {
                    log.error("Insufficient stock for product: {}", orderItem.getProductId());
                    // 记录失败的交易
                    recordStockTransaction(orderId, orderItem, "RESERVE", "FAILED", "Insufficient stock");
                    return false;
                }

                // 更新库存
                UpdateWrapper<StockVO> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("product_id", orderItem.getProductId())
                        .setSql("reserved_quantity = reserved_quantity + " + orderItem.getQuantity());

                if (stockMapper.update(null, updateWrapper) != 1) {
                    log.error("Failed to reserve stock for product: {}", orderItem.getProductId());
                    recordStockTransaction(orderId, orderItem, "RESERVE", "FAILED", "Update failed");
                    throw new RuntimeException("Failed to reserve stock for product: " + orderItem.getProductId());
                }

                // 记录成功的交易
                recordStockTransaction(orderId, orderItem, "RESERVE", "SUCCESS", null);

                // 更新缓存
                stockCache.updateReservedQuantity(orderItem.getProductId(), orderItem.getQuantity());
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to reserve stock", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean releaseStock(String orderId, List<OrderItem> list) {
        if (orderId == null || orderId.isEmpty() || list == null || list.isEmpty()) {
            return false;
        }

        try {
            for (OrderItem orderItem : list) {
                // 先查询库存
                QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("product_id", orderItem.getProductId());
                StockVO stock = stockMapper.selectOne(queryWrapper);

                // 检查库存是否存在且预留数量足够
                if (stock == null || stock.getReservedQuantity() < orderItem.getQuantity()) {
                    log.error("Invalid stock state for release: {}", orderItem.getProductId());
                    recordStockTransaction(orderId, orderItem, "RELEASE", "FAILED", "Insufficient reserved quantity");
                    return false;
                }

                // 更新库存
                UpdateWrapper<StockVO> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("product_id", orderItem.getProductId())
                        .setSql("reserved_quantity = reserved_quantity - " + orderItem.getQuantity())
                        .ge("reserved_quantity", 0);

                if (stockMapper.update(null, updateWrapper) != 1) {
                    log.error("Failed to release stock for product: {}", orderItem.getProductId());
                    recordStockTransaction(orderId, orderItem, "RELEASE", "FAILED", "Update failed");
                    throw new RuntimeException("Failed to release stock for product: " + orderItem.getProductId());
                }

                // 记录成功的交易
                recordStockTransaction(orderId, orderItem, "RELEASE", "SUCCESS", null);

                // 更新缓存
                stockCache.updateReservedQuantity(orderItem.getProductId(), -orderItem.getQuantity());
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to release stock", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean delete(String productId) {
        if (productId == null || productId.isEmpty()) {
            return false;
        }

        try {
            // 删除数据库记录
            QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("product_id", productId);
            boolean success = stockMapper.delete(queryWrapper) == 1;

            // 删除缓存
            if (success) {
                stockCache.removeFromCache(productId);
            }

            return success;
        } catch (Exception e) {
            log.error("Failed to delete stock: {}", productId, e);
            return false;
        }
    }

    // 记录库存交易
    private void recordStockTransaction(String orderId, OrderItem orderItem, String operationType,
                                        String status, String errorMessage) {
        StockTransactionVO transaction = new StockTransactionVO();
        transaction.setOrderId(orderId);
        transaction.setProductId(orderItem.getProductId());
        transaction.setOperationType(operationType);
        transaction.setQuantity(orderItem.getQuantity());
        transaction.setUnitPrice(orderItem.getPrice());
        transaction.setTotalAmount(orderItem.getPrice().multiply(new BigDecimal(orderItem.getQuantity())));
        transaction.setStatus(status);
        transaction.setErrorMessage(errorMessage);
        stockTransactionMapper.insert(transaction);
    }
}
