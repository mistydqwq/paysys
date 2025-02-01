package com.paysys.order.ports.inbound;

import com.paysys.order.common.BaseResponse;

public interface GetOrderStatusUseCase {
    BaseResponse<Integer> getOrderStatus(String orderId);
}
