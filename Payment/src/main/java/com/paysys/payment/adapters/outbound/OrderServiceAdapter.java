package com.paysys.payment.adapters.outbound;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import com.paysys.order.OrderServiceApi;

@Service
public class OrderServiceAdapter implements OrderServiceApi {

    @DubboReference(version = "1.0.0", check = false)
    private OrderServiceApi orderServiceApi;

    @Override
    public Boolean updateOrderStatus(String orderId, int orderStatus) {
        return orderServiceApi.updateOrderStatus(orderId, orderStatus);
    }

    @Override
    public Boolean updatePaymentLink(String orderId, String paymentLink) {
        return orderServiceApi.updatePaymentLink(orderId, paymentLink);
    }
}
