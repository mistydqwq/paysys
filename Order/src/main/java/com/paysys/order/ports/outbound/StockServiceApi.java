package com.paysys.order.ports.outbound;

import com.paysys.order.common.BaseResponse;
import com.paysys.order.domain.valueobj.OrderItem;

import java.util.List;

public interface StockServiceApi {
    BaseResponse<Boolean> reserveStock(String orderId, List<OrderItem>list);
    boolean releaseStock(String orderId, List<OrderItem>list);
}
