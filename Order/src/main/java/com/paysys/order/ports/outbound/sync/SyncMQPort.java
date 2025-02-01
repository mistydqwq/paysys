package com.paysys.order.ports.outbound.sync;

import com.paysys.order.adapters.outbound.sync.RedisMQVO;
import org.redisson.api.StreamMessageId;

import java.util.List;
import java.util.Map;

public interface SyncMQPort {
    boolean publishMessage(String streamKey, Map<String, String> message);
    void createConsumerGroup(String streamKey, String GroupName);
    List<RedisMQVO> consumeMessage(String streamKey, String groupName, String consumerName, int count, long blockTimeMs);
    boolean ackMessage(String streamKey, String groupName, StreamMessageId messageId);
}
