package com.paysys.stock.application.commands;

import com.paysys.stock.ports.inbound.ReleaseStockUseCase;
import com.paysys.stock.ports.outbound.DistributedLockPort;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import com.paysys.vo.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReleaseStockHandler implements ReleaseStockUseCase {

    @Qualifier("redisRepositoryImpl")
    @Autowired
    private StockRepositoryPort stockRepositoryPort;

    @Autowired
    private DistributedLockPort distributedLockPort;

    @Override
    @Transactional
    public Boolean releaseStock(String orderId, List<OrderItem> list) {
        if (orderId == null || orderId.isEmpty() || list == null || list.isEmpty()) {
            return false;
        }

        boolean hasProcessed = stockRepositoryPort.checkStockTransaction(orderId, "RELEASE", "SUCCESS");
        if (hasProcessed) {
            log.warn("Order: {} has been processed", orderId);
            return true;
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
                    return false;
                }
                lockedProducts.add(productId);
            }
            return stockRepositoryPort.releaseStock(orderId, list);
        } catch (Exception e) {
            log.error("Failed to release stock for order: {}, error: {}", orderId, e.getMessage());
            return false;
        } finally {
            lockedProducts.forEach(distributedLockPort::releaseLock);
        }
    }
}
