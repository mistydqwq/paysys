package com.paysys.payment.application.commands;

import lombok.Data;

@Data
public class PaymentResultCommand {
    private String transactionId;
    private String channelTransactionId;
    private String tradeStatus;
    private String amount;
    private String errorCode;
    private String errorMsg;
}
