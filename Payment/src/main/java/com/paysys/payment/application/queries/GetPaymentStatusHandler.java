package com.paysys.payment.application.queries;

import com.paysys.common.BaseResponse;
import com.paysys.common.ErrorCode;
import com.paysys.common.ResultUtils;
import com.paysys.payment.domain.entities.Payment;
import com.paysys.payment.ports.inbound.GetPaymentStatusUseCase;
import com.paysys.payment.ports.outbound.PaymentRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetPaymentStatusHandler implements GetPaymentStatusUseCase {

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;

    @Override
    public BaseResponse<Integer> getPaymentStatus(String paymentId) {
        Optional<Payment> payment = paymentRepositoryPort.findById(paymentId);
        if (!payment.isPresent()) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Payment not found");
        }
        return ResultUtils.success(payment.get().getTransactionStatus().getCode());
    }
}