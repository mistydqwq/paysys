package com.paysys.order.domain.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    CREATED(0, "Created"),
    PAID(1, "Paid"),
    FAILED(2, "Failed"),
    CANCELLED(3, "Cancelled"),
    PENDING(4, "Pending");

    private final int code;
    private final String description;

    OrderStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OrderStatusEnum fromCode(int code) {
        for (OrderStatusEnum status : OrderStatusEnum.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid code for OrderStatusEnum: " + code);
    }
}
