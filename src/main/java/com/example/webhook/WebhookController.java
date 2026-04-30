import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final WebhookDispatcher webhookDispatcher;

    @Autowired
    public WebhookController(WebhookDispatcher webhookDispatcher) {
        this.webhookDispatcher = webhookDispatcher;
    }

    @PostMapping
    public ResponseEntity<String> registerWebhook(@RequestBody Webhook webhook, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        webhookDispatcher.dispatchWebhook(webhook, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body("Webhook registered successfully.");
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        // Logic to return the status of webhook processing (to be implemented).
        return ResponseEntity.ok("Webhook service is running.");
    }

    @PostMapping("/new")
    public ResponseEntity<String> registerNewWebhook(@RequestBody NewWebhook newWebhook, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // Handle the new webhook registration logic here
        webhookDispatcher.dispatchNewWebhook(newWebhook, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body("New webhook registered successfully.");
    }

    @PostMapping("/notifications")
    public ResponseEntity<String> handleWebhookNotification(@RequestBody Map<String, Object> notification) {
        String eventType = (String) notification.get("eventType");
        Object payload = notification.get("payload");

        // Process the eventType and payload as needed
        // For example, dispatch or log the notification

        return ResponseEntity.ok("Notification received successfully.");
    }
}
