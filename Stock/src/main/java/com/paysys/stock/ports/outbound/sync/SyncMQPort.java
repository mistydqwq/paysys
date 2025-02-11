package com.paysys.stock.ports.outbound.sync;

public interface SyncMQPort {
    boolean publishMessage(String redisKey, String operationType);
}
