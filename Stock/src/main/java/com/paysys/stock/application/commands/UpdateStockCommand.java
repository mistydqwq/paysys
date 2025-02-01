package com.paysys.stock.application.commands;

import lombok.Data;


import java.math.BigDecimal;

@Data
public class UpdateStockCommand {
    private String productId;
    private BigDecimal price;
    private Long quantity;
}