package com.paysys.order.application.commands;

import com.paysys.order.ports.inbound.UpdatePaymentLinkUseCase;
import com.paysys.order.ports.outbound.OrderRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UpdatePaymentLinkHandler implements UpdatePaymentLinkUseCase {

    @Autowired
    @Qualifier("redisRepositoryImpl")
    private OrderRepositoryPort orderRepositoryPort;

    @Override
    public Boolean updatePaymentLink(String orderId, String paymentLink) {
        return orderRepositoryPort.updatePaymentLink(orderId, paymentLink);
    }
}
