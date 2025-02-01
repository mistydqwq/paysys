package com.paysys.stock.ports.inbound;

import com.paysys.common.BaseResponse;
import com.paysys.vo.OrderItem;

import java.util.List;

public interface ReserveStockUseCase {
    BaseResponse<Boolean> reserveStock(String orderId, List<OrderItem> list);
}