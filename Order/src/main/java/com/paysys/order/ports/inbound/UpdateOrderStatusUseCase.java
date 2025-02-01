package com.paysys.order.ports.inbound;

public interface UpdateOrderStatusUseCase {
    Boolean updateOrderStatus(String orderId, int status);
}
