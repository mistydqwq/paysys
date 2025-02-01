package com.paysys.order.application.queries;

import com.paysys.common.BaseResponse;
import com.paysys.common.ErrorCode;
import com.paysys.common.ResultUtils;
import com.paysys.order.domain.entities.Order;
import com.paysys.order.ports.inbound.GetOrderStatusUseCase;
import com.paysys.order.ports.outbound.OrderRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetOrderStatusHandler implements GetOrderStatusUseCase {

    @Autowired
    private OrderRepositoryPort orderRepositoryPort;

    @Override
    public BaseResponse<Integer> getOrderStatus(String orderId) {
        Optional<Order> optionalOrder = orderRepositoryPort.findById(orderId);
        if (!optionalOrder.isPresent()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "Order不存在");
        }

        Order order = optionalOrder.get();
        return ResultUtils.success(order.getStatus().getCode());
    }
}
