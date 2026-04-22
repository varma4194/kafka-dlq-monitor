package dev.ravi.dlq.service;

import dev.ravi.dlq.model.DeadMessage;
import dev.ravi.dlq.model.DeadMessage.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageStore {
    private final Map<String, DeadMessage> store = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void add(DeadMessage msg) {
        store.put(msg.getId(), msg);
        log.info("Stored dead message: id={}, topic={}, error={}", msg.getId(), msg.getOriginalTopic(), msg.getErrorMessage());
    }

    public List<DeadMessage> getAll() {
        return new ArrayList<>(store.values());
    }

    public DeadMessage getById(String id) {
        return store.get(id);
    }

    public List<DeadMessage> getByStatus(MessageStatus status) {
        return store.values().stream()
                .filter(m -> m.getStatus() == status)
                .collect(Collectors.toList());
    }

    public void retry(String id) {
        DeadMessage msg = store.get(id);
        if (msg == null) {
            throw new IllegalArgumentException("Message not found: " + id);
        }

        try {
            kafkaTemplate.send(msg.getOriginalTopic(), msg.getKey(), msg.getPayload()).get();
            msg.setStatus(MessageStatus.RETRIED);
            log.info("Retried message {} to topic {}", id, msg.getOriginalTopic());
        } catch (Exception e) {
            log.error("Failed to retry message {}", id, e);
            throw new RuntimeException("Failed to retry message", e);
        }
    }

    public void discard(String id) {
        DeadMessage msg = store.get(id);
        if (msg == null) {
            throw new IllegalArgumentException("Message not found: " + id);
        }
        msg.setStatus(MessageStatus.DISCARDED);
        log.info("Discarded message {}", id);
    }

    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", store.size());
        stats.put("pending", (int) store.values().stream().filter(m -> m.getStatus() == MessageStatus.PENDING).count());
        stats.put("retried", (int) store.values().stream().filter(m -> m.getStatus() == MessageStatus.RETRIED).count());
        stats.put("discarded", (int) store.values().stream().filter(m -> m.getStatus() == MessageStatus.DISCARDED).count());
        return stats;
    }

    // TODO: persist to DB for multi-instance setups
}
