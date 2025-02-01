package com.paysys.order.application.commands;

import lombok.Data;

@Data
public class CreateOrderCommand {
    private String customerId;
    private String items;
    private String note;
}
