package com.paysys.order.application.commands;

import com.paysys.order.ports.inbound.UpdateOrderStatusUseCase;
import com.paysys.order.ports.outbound.OrderRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UpdateOrderStatusHandler implements UpdateOrderStatusUseCase {

    @Autowired
    @Qualifier("redisRepositoryImpl")
    private OrderRepositoryPort orderRepositoryPort;

    @Override
    public Boolean updateOrderStatus(String orderId, int status) {
        return orderRepositoryPort.updateStatus(orderId, status);
    }
}
