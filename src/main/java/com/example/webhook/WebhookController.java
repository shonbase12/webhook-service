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

    @PostMapping("/validate")
    public ResponseEntity<String> validateWebhook(@RequestBody Webhook webhook) {
        // Add your validation logic here
        boolean isValid = webhookDispatcher.validateWebhook(webhook);

        if (isValid) {
            return ResponseEntity.ok("Webhook is valid.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook validation failed.");
        }
    }

    @PostMapping("/process-event")
    public ResponseEntity<String> processWebhookEvent(@RequestBody Map<String, Object> event) {
        // Extract data from the event map
        String eventType = (String) event.get("eventType");
        Object eventData = event.get("data");

        // Implement your specific functionality here
        // For example, process the event based on eventType and eventData

        // Return a success response
        return ResponseEntity.ok("Webhook event processed successfully.");
    }

    /**
     * Handles a specific webhook action.
     *
     * @param specificEvent the specific webhook event to handle
     * @return ResponseEntity with status and message indicating the result
     */
    @PostMapping("/specific-action")
    public ResponseEntity<String> handleSpecificWebhookAction(@RequestBody SpecificWebhookEvent specificEvent) {
        try {
            // Add logic to handle the specific webhook action
            webhookDispatcher.dispatchSpecificWebhookEvent(specificEvent);
            return ResponseEntity.status(HttpStatus.CREATED).body("Specific webhook action handled successfully.");
        } catch (Exception e) {
            // Log the exception (logging not shown here)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to handle specific webhook action.");
        }
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateWebhook(@RequestBody Webhook webhook, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            webhookDispatcher.dispatchUpdateWebhook(webhook, idempotencyKey);
            return ResponseEntity.ok("Webhook updated successfully.");
        } catch (Exception e) {
            // Log the exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update webhook.");
        }
    }

    @PostMapping("/handle-new-event")
    public ResponseEntity<String> handleNewWebhookEvent(@RequestBody NewWebhookEvent newEvent) {
        try {
            // Delegate processing to the webhookDispatcher
            webhookDispatcher.dispatchNewWebhookEvent(newEvent);
            return ResponseEntity.status(HttpStatus.CREATED).body("New webhook event handled successfully.");
        } catch (Exception e) {
            // Log the exception (logging code not shown here)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to handle new webhook event.");
        }
    }
}
