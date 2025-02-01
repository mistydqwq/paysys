package com.paysys.order;

public interface OrderServiceApi {
    Boolean updateOrderStatus(String orderId, int status);
    Boolean updatePaymentLink(String orderId, String paymentLink);
}
