import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

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
}