package com.paysys.stock.adapters.outbound;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.domain.valueobj.StockTransactionVO;
import com.paysys.stock.domain.valueobj.StockVO;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.paysys.vo.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("dbRepositoryImpl")
public class DBRepositoryImpl implements StockRepositoryPort {

    private final StockMapper stockMapper;
    private final StockTransactionMapper stockTransactionMapper;

    // 构造函数去除了StockCache依赖
    public DBRepositoryImpl(StockMapper stockMapper,
                            StockTransactionMapper stockTransactionMapper) {
        this.stockMapper = stockMapper;
        this.stockTransactionMapper = stockTransactionMapper;
    }

    @Override
    public Optional<Stock> findById(String productId) {
        if (productId == null || productId.isEmpty()) {
            return Optional.empty();
        }

        // 直接查询数据库
        QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        return Optional.ofNullable(stockMapper.selectOne(queryWrapper))
                .map(Stock::fromVO);
    }

    @Override
    @Transactional
    public boolean save(Stock stock) {
        if (stock == null) {
            return false;
        }

        try {
            // 仅操作数据库
            return stockMapper.insert(stock.toVO()) == 1;
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
            UpdateWrapper<StockVO> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("product_id", stock.getProductId());
            StockVO stockVO = stock.toVO();
            return stockMapper.update(stockVO, updateWrapper) == 1;
        } catch (Exception e) {
            log.error("Failed to update stock: {}", stock.getProductId(), e);
            return false;
        }
    }

    @Override
    public boolean checkStockTransaction(String orderId, String operationType, String status) {
        QueryWrapper<StockTransactionVO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId)
                .eq("operation_type", operationType)
                .eq("status", status);
        return stockTransactionMapper.exists(queryWrapper);
    }

    @Override
    @Transactional
    public boolean reserveStock(String orderId, List<OrderItem> list) {
        if (orderId == null || orderId.isEmpty() || list == null || list.isEmpty()) {
            return false;
        }

        try {
            for (OrderItem orderItem : list) {
                QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("product_id", orderItem.getProductId());
                StockVO stock = stockMapper.selectOne(queryWrapper);

                if (stock == null || stock.getQuantity() < stock.getReservedQuantity() + orderItem.getQuantity()) {
                    log.error("Insufficient stock for product: {}", orderItem.getProductId());
                    recordStockTransaction(orderId, orderItem, "RESERVE", "FAILED", "Insufficient stock");
                    return false;
                }

                UpdateWrapper<StockVO> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("product_id", orderItem.getProductId())
                        .setSql("reserved_quantity = reserved_quantity + " + orderItem.getQuantity());

                if (stockMapper.update(null, updateWrapper) != 1) {
                    log.error("Failed to reserve stock for product: {}", orderItem.getProductId());
                    recordStockTransaction(orderId, orderItem, "RESERVE", "FAILED", "Update failed");
                    throw new RuntimeException("Failed to reserve stock for product: " + orderItem.getProductId());
                }

                recordStockTransaction(orderId, orderItem, "RESERVE", "SUCCESS", null);
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
                QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("product_id", orderItem.getProductId());
                StockVO stock = stockMapper.selectOne(queryWrapper);

                if (stock == null || stock.getReservedQuantity() < orderItem.getQuantity()) {
                    log.error("Invalid stock state for release: {}", orderItem.getProductId());
                    recordStockTransaction(orderId, orderItem, "RELEASE", "FAILED", "Insufficient reserved quantity");
                    return false;
                }

                UpdateWrapper<StockVO> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("product_id", orderItem.getProductId())
                        .setSql("reserved_quantity = reserved_quantity - " + orderItem.getQuantity())
                        .ge("reserved_quantity", 0);

                if (stockMapper.update(null, updateWrapper) != 1) {
                    log.error("Failed to release stock for product: {}", orderItem.getProductId());
                    recordStockTransaction(orderId, orderItem, "RELEASE", "FAILED", "Update failed");
                    throw new RuntimeException("Failed to release stock for product: " + orderItem.getProductId());
                }

                recordStockTransaction(orderId, orderItem, "RELEASE", "SUCCESS", null);
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
            QueryWrapper<StockVO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("product_id", productId);
            return stockMapper.delete(queryWrapper) == 1;
        } catch (Exception e) {
            log.error("Failed to delete stock: {}", productId, e);
            return false;
        }
    }

    // 保留事务记录逻辑
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