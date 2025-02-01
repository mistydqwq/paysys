package com.paysys.order.ports.inbound;

public interface UpdatePaymentLinkUseCase {
    Boolean updatePaymentLink(String orderId, String paymentLink);
}
