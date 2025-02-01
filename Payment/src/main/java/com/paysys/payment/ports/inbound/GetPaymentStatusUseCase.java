package com.paysys.payment.ports.inbound;

import com.paysys.payment.common.BaseResponse;

public interface GetPaymentStatusUseCase {
    BaseResponse<Integer> getPaymentStatus(String paymentId);
}
