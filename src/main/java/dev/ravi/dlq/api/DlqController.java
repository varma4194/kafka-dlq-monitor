package dev.ravi.dlq.api;

import dev.ravi.dlq.model.DeadMessage;
import dev.ravi.dlq.service.MessageStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class DlqController {
    private final MessageStore store;

    @GetMapping
    public ResponseEntity<List<DeadMessage>> listMessages() {
        return ResponseEntity.ok(store.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeadMessage> getMessage(@PathVariable String id) {
        DeadMessage msg = store.getById(id);
        if (msg == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retryMessage(@PathVariable String id) {
        try {
            store.retry(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<Void> discardMessage(@PathVariable String id) {
        try {
            store.discard(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Integer>> getStats() {
        return ResponseEntity.ok(store.getStats());
    }
}
