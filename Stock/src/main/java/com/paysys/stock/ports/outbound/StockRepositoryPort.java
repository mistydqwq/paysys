package com.paysys.stock.ports.outbound;

import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.domain.valueobj.OrderItem;

import java.util.List;
import java.util.Optional;

public interface StockRepositoryPort {
    Optional<Stock> findById(String productId);
    boolean save(Stock stock);
    boolean update(Stock stock);
    boolean reserveStock(String orderId, List<OrderItem> list);
    boolean releaseStock(String orderId, List<OrderItem> list);
    boolean delete(String productId);
}