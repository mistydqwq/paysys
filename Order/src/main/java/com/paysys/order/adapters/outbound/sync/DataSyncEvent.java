package com.paysys.order.adapters.outbound.sync;

import lombok.Getter;

@Getter
public class DataSyncEvent {
    private String key;          // Redis键（如 "order:123"）
    private String operation;    // 操作类型（CREATE/UPDATE/DELETE）
    private String dataType;     // 数据类型（根据key解析得到，如 "order"）
    private Long timestamp;      // 事件时间戳

    // 构造方法和getter/setter
    public DataSyncEvent(String key, String operation) {
        this.key = key;
        this.operation = operation;
        this.timestamp = System.currentTimeMillis();
        this.dataType = key.split(":")[0];
    }
}
