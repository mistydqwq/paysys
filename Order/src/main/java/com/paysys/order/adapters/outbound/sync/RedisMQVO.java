package com.paysys.order.adapters.outbound.sync;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RedisMQVO {
    private String messageId;
    private Map<String, String> message;
}
