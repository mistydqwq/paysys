package com.paysys.order.adapters.outbound.sync;

import com.paysys.order.ports.outbound.sync.SyncMQPort;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SyncMQPortImpl implements SyncMQPort {

    private final RedissonClient redissonClient;

    public SyncMQPortImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean publishMessage(String streamKey, Map<String, String> message) {
        try {
            RStream<String, String> stream = redissonClient.getStream(streamKey);
            StreamMessageId messageId = stream.add(StreamAddArgs.entries(message));
            return messageId != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void createConsumerGroup(String streamKey, String groupName) {
        try{
            RStream<String, String> stream = redissonClient.getStream(streamKey);
            stream.createGroup(StreamCreateGroupArgs.name(groupName));
        } catch (Exception e) {
            if (e.getMessage() != null && !e.getMessage().contains("BUSYGROUP")) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<RedisMQVO> consumeMessage(String streamKey, String groupName, String consumerName, int count, long blockTimeMs){
        List<RedisMQVO> msgs = new ArrayList<>();
        try {
            RStream<String, String> stream = redissonClient.getStream(streamKey);
            Map<StreamMessageId, Map<String, String>> readResult =
                    stream.readGroup(groupName, consumerName, StreamReadGroupArgs.neverDelivered().count(count).timeout(Duration.ofMillis(blockTimeMs)));

            if (readResult != null) {
                for (Map.Entry<StreamMessageId, Map<String, String>> entry : readResult.entrySet()) {
                    StreamMessageId smId = entry.getKey();
                    Map<String, String> body = entry.getValue();
                    msgs.add(new RedisMQVO(smId.toString(), body));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msgs;
    }

    @Override
    public boolean ackMessage(String streamKey, String groupName, StreamMessageId messageId) {
        try {
            RStream<String, String> stream = redissonClient.getStream(streamKey);
            long ackCount = stream.ack(groupName, messageId);
            return ackCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
