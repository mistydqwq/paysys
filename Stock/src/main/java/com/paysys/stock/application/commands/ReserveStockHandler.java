package com.paysys.stock.application.commands;

import com.paysys.common.BaseResponse;
import com.paysys.common.ErrorCode;
import com.paysys.common.ResultUtils;
import com.paysys.stock.ports.inbound.ReserveStockUseCase;
import com.paysys.stock.ports.outbound.DistributedLockPort;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.paysys.vo.OrderItem;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReserveStockHandler implements ReserveStockUseCase {

    @Qualifier("redisRepositoryImpl")
    @Autowired
    private StockRepositoryPort stockRepositoryPort;

    @Autowired
    private DistributedLockPort distributedLockPort;

    @Override
    @Transactional
    public BaseResponse<Boolean> reserveStock(String orderId, List<OrderItem> list) {
        if (orderId == null || orderId.isEmpty() || list == null || list.isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Invalid order id");
        }
        
        boolean hasProcessed = stockRepositoryPort.checkStockTransaction(orderId, "RESERVE", "SUCCESS");
        if (hasProcessed) {
            log.warn("Order: {} has been processed", orderId);
            return ResultUtils.success(true);
        }

        List<String> productIds = list.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<String> lockedProducts = new ArrayList<>();
        try {
            for (String productId : productIds) {
                if (!distributedLockPort.acquireLock(productId, 3)) {
                    log.error("Failed to acquire lock for product: {}", productId);
                    lockedProducts.forEach(distributedLockPort::releaseLock);
                    return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Failed to acquire lock for product: " + productId);
                }
                lockedProducts.add(productId);
            }
            boolean success = stockRepositoryPort.reserveStock(orderId, list);
            return success ?
                    ResultUtils.success(true) :
                    ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Failed to reserve stock");
        } catch (Exception e) {
            log.error("Failed to reserve stock for order: {}, error: {}", orderId, e.getMessage());
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Failed to reserve stock");
        } finally {
            lockedProducts.forEach(distributedLockPort::releaseLock);
        }
    }
}
