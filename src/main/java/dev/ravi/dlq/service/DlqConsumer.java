package dev.ravi.dlq.service;

import dev.ravi.dlq.model.DeadMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqConsumer {
    private final MessageStore store;

    @KafkaListener(
            topics = "${dlq.topics:}",
            groupId = "${spring.kafka.consumer.group-id:dlq-monitor}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDlq(
            @Payload String payload,
            ConsumerRecord<String, String> record,
            @Header(name = "kafka_dlt-original-topic", required = false) String originalTopic,
            @Header(name = "kafka_dlt-exception-message", required = false) String exceptionMsg,
            @Header(name = "kafka_dlt-exception-stacktrace", required = false) String stackTrace) {

        try {
            String topic = originalTopic != null ? originalTopic : "unknown";
            String errorMsg = exceptionMsg != null ? exceptionMsg : "Unknown error";

            DeadMessage msg = DeadMessage.create(
                    topic,
                    record.partition(),
                    record.offset(),
                    record.key(),
                    payload,
                    errorMsg
            );

            store.add(msg);
            log.debug("Consumed DLQ message: id={}, original_topic={}, partition={}, offset={}",
                    msg.getId(), topic, record.partition(), record.offset());

        } catch (Exception e) {
            log.error("Error processing DLQ message", e);
        }
    }
}
