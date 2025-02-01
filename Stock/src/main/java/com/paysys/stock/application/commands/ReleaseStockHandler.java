package com.paysys.stock.application.commands;

import com.paysys.stock.domain.valueobj.OrderItem;
import com.paysys.stock.ports.inbound.ReleaseStockUseCase;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ReleaseStockHandler implements ReleaseStockUseCase {

    @Autowired
    private StockRepositoryPort stockRepositoryPort;

    @Override
    @Transactional
    public Boolean releaseStock(String orderId, List<OrderItem> list) {
        if (orderId == null || orderId.isEmpty() || list == null || list.isEmpty()) {
            return false;
        }

        try {
            return stockRepositoryPort.releaseStock(orderId, list);
        } catch (Exception e) {
            log.error("Failed to release stock for order: {}, error: {}", orderId, e.getMessage());
            return false;
        }
    }
}
