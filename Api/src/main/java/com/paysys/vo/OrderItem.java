package com.paysys.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItem implements java.io.Serializable {
    private final String productId;
    private final String productName;
    private final Long quantity;
    private final BigDecimal price;
}
