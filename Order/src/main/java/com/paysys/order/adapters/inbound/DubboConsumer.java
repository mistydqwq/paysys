package com.paysys.order.adapters.inbound;

import com.paysys.order.OrderServiceApi;
import com.paysys.order.ports.inbound.UpdateOrderStatusUseCase;
import com.paysys.order.ports.inbound.UpdatePaymentLinkUseCase;
import org.apache.dubbo.config.annotation.DubboService;


@DubboService(version = "1.0.0")
public class DubboConsumer implements OrderServiceApi {
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final UpdatePaymentLinkUseCase updatePaymentLinkUseCase;

    public DubboConsumer(UpdateOrderStatusUseCase updateOrderStatusUseCase, UpdatePaymentLinkUseCase updatePaymentLinkUseCase) {
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
        this.updatePaymentLinkUseCase = updatePaymentLinkUseCase;
    }
    @Override
    public Boolean updateOrderStatus(String orderId, int status) {
        return updateOrderStatusUseCase.updateOrderStatus(orderId, status);
    }
    @Override
    public Boolean updatePaymentLink(String orderId, String paymentLink) {
        return updatePaymentLinkUseCase.updatePaymentLink(orderId, paymentLink);
    }
}
