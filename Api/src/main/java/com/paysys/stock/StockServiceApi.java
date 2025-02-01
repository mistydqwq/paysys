package com.paysys.stock;

import com.paysys.vo.OrderItem;
import com.paysys.common.BaseResponse;

import java.util.List;

public interface StockServiceApi {
    BaseResponse<Boolean> reserveStock(String orderId, List<OrderItem>list);
    boolean releaseStock(String orderId, List<OrderItem>list);
}
