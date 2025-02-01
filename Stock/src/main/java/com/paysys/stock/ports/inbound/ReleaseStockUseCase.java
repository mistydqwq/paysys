package com.paysys.stock.ports.inbound;

import com.paysys.stock.domain.valueobj.OrderItem;

import java.util.List;

public interface ReleaseStockUseCase {
    Boolean releaseStock(String orderId, List<OrderItem> list);
}