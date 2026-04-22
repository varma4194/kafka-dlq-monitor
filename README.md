# Kafka DLQ Monitor

A lightweight Spring Boot application for monitoring and managing Kafka dead letter queues. Captures failed messages from configured DLT topics, stores them in memory, and provides a web UI to inspect, retry, or discard messages.

## Features

- **Real-time DLQ Monitoring**: Automatically consumes from configured dead letter topics
- **Web Dashboard**: Single-page app to view dead messages with live auto-refresh
- **Message Retry**: Republish failed messages back to their original topics
- **Message Discard**: Mark messages as discarded without further action
- **Statistics**: View counts of pending, retried, and discarded messages
- **Error Tracking**: Captures original error messages from Kafka headers

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Kafka broker running on `localhost:9092` (or configure in `application.yml`)

### Build

```bash
mvn clean package
```

### Run

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8090`.

### Configure DLQ Topics

Edit `src/main/resources/application.yml`:

```yaml
dlq:
  topics: my-dlq-topic-1,my-dlq-topic-2,payment-dlq
```

Or set via environment variable:

```bash
export DLQ_TOPICS="my-dlq-1,my-dlq-2"
mvn spring-boot:run
```

### Kafka Configuration

The application expects dead letter topics to be published by Spring Kafka's `DeadLetterPublishingRecoverer`. It extracts error context from standard Kafka headers:

- `kafka_dlt-original-topic`: Original topic before failure
- `kafka_dlt-exception-message`: Error message
- `kafka_dlt-exception-stacktrace`: Exception stack trace (captured but not shown in UI)

## API Endpoints

### List all messages
```
GET /api/messages
```

### Get specific message
```
GET /api/messages/{id}
```

### Retry a message
```
POST /api/messages/{id}/retry
```

### Discard a message
```
POST /api/messages/{id}/discard
```

### Get statistics
```
GET /api/messages/stats
```

Response:
```json
{
  "total": 15,
  "pending": 8,
  "retried": 5,
  "discarded": 2
}
```

## Web Dashboard

Navigate to `http://localhost:8090` to access the dashboard.

**Features:**
- Real-time stats showing total, pending, retried, and discarded message counts
- Table view of all dead messages with topic, timestamp, error, and status
- Payload preview (truncated to 60 chars)
- One-click retry or discard actions
- Auto-refresh every 10 seconds
- Manual refresh button

## Architecture

```
DlqConsumer
  ↓
  └→ Listens to configured DLQ topics via @KafkaListener
     └→ Parses ConsumerRecord and extracts headers
        └→ Creates DeadMessage objects

MessageStore
  ↓
  └→ In-memory ConcurrentHashMap storage
     ├→ add(msg) - stores new message
     ├→ getAll() - returns all messages
     ├→ retry(id) - republishes via KafkaTemplate
     └→ discard(id) - marks as discarded

DlqController
  ↓
  └→ REST API serving dashboard and client requests

index.html
  ↓
  └→ Vanilla JS frontend with auto-refresh
```

## Notes

- **Storage**: Messages are stored in-memory. Restarts will clear all data.
  - **TODO**: Persist to database for multi-instance setups
- **Performance**: Tested with thousands of messages; no pagination yet
- **Serialization**: Assumes message payloads are JSON or text; binary payloads will be Base64 encoded by Spring Kafka
- **Error Headers**: Relies on `DeadLetterPublishingRecoverer` header conventions

## Spring Kafka Configuration Notes

To use this monitor, ensure your application's error handler is configured:

```java
@Bean
public ConsumerRecordRecoverer recoverer(KafkaTemplate<String, String> template) {
    return new DeadLetterPublishingRecoverer(template,
        (record, ex) -> new TopicPartition(record.getTopic() + ".dlq", 0));
}
```

This automatically publishes failed records to DLQ topics and sets the required headers.

## License

MIT
