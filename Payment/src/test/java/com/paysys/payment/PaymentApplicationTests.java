package com.paysys.payment;

import com.paysys.payment.application.commands.CreatePaymentCommand;
import com.paysys.payment.ports.inbound.CreatePaymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
class PaymentApplicationTests {

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Test
    public void testCreatePayment() {
        CreatePaymentCommand createPaymentCommand = new CreatePaymentCommand();
        createPaymentCommand.setOrderId("123456");
        createPaymentCommand.setAmount(new BigDecimal("100.00"));
        boolean res = createPaymentUseCase.createPayment(createPaymentCommand);
        System.out.println(res);
    }

}
