package com.paysys.order.ports.inbound;

import com.paysys.order.application.commands.CreateOrderCommand;
import com.paysys.common.BaseResponse;

public interface CreateOrderUseCase {
    BaseResponse<String> createOrder(CreateOrderCommand createOrderCommand);
}
