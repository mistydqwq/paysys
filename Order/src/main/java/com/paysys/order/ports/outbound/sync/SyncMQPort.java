package com.paysys.order.ports.outbound.sync;

import java.util.Map;

public interface SyncMQPort {
    boolean publishMessage(String redisKey, String operationType);
}
