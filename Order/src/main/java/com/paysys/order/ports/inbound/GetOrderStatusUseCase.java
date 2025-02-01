package com.paysys.order.ports.inbound;

import com.paysys.common.BaseResponse;

public interface GetOrderStatusUseCase {
    BaseResponse<Integer> getOrderStatus(String orderId);
}
