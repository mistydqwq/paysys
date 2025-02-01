package com.paysys.payment.ports.inbound;

import com.paysys.common.BaseResponse;

public interface GetPaymentStatusUseCase {
    BaseResponse<Integer> getPaymentStatus(String paymentId);
}
