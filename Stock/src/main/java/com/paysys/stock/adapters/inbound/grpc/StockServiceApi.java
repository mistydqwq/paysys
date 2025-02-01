package com.paysys.stock.adapters.inbound.grpc;


import com.paysys.stock.common.BaseResponse;
import com.paysys.stock.domain.valueobj.OrderItem;

import java.util.List;

public interface StockServiceApi {
    BaseResponse<Boolean> reserveStock(String orderId, List<OrderItem>list);
    Boolean releaseStock(String orderId, List<OrderItem>list);
}
