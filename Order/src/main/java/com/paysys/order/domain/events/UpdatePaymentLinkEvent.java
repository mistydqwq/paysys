package com.paysys.order.domain.events;

import lombok.Data;

@Data
public class UpdatePaymentLinkEvent {
    private final String orderId;
    private final String paymentLink;
}
