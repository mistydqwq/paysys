package com.paysys.payment.domain.enums;

public enum PaymentChannelEnum {
    ALIPAY("0", "alipay"),
    WECHAT("1", "wechat"),
    STRIPE("2", "stripe"),
    PAYPAL("3", "paypal");

    private final String code;
    private final String description;

    PaymentChannelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentChannelEnum fromCode(String code) {
        for (PaymentChannelEnum channel : PaymentChannelEnum.values()) {
            if (channel.getCode().equals(code)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Invalid PaymentChannel code: " + code);
    }
}
