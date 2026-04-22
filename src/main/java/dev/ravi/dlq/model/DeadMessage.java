package dev.ravi.dlq.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeadMessage {
    private String id;
    private String originalTopic;
    private int partition;
    private long offset;
    private String key;
    private String payload;
    private String errorMessage;
    private Instant receivedAt;
    private MessageStatus status;

    public enum MessageStatus {
        PENDING, RETRIED, DISCARDED
    }

    public static DeadMessage create(String topic, int partition, long offset, String key, String payload, String errorMsg) {
        DeadMessage msg = new DeadMessage();
        msg.setId(UUID.randomUUID().toString());
        msg.setOriginalTopic(topic);
        msg.setPartition(partition);
        msg.setOffset(offset);
        msg.setKey(key);
        msg.setPayload(payload);
        msg.setErrorMessage(errorMsg);
        msg.setReceivedAt(Instant.now());
        msg.setStatus(MessageStatus.PENDING);
        return msg;
    }
}
