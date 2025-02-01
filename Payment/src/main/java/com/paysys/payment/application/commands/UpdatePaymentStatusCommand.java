package com.paysys.payment.application.commands;

import com.paysys.payment.domain.enums.PaymentStatusEnum;
import lombok.Data;

@Data
public class UpdatePaymentStatusCommand {
    private String transactionId;
    private PaymentStatusEnum transactionStatus;
    private String errorCode;
    private String errorMsg;
    private String channelTransactionId;
}
