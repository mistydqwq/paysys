package com.paysys.stock.ports.inbound;

import com.paysys.stock.common.BaseResponse;
import com.paysys.stock.domain.valueobj.OrderItem;

import java.util.List;

public interface ReserveStockUseCase {
    BaseResponse<Boolean> reserveStock(String orderId, List<OrderItem> list);
}