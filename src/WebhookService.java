public class WebhookService {

    private final WebhookDispatcher webhookDispatcher;

    public WebhookService() {
        // Initialize WebhookDispatcher with a default maxRetries value, e.g., 3
        this.webhookDispatcher = new WebhookDispatcher(3);
    }

    /**
     * Register a new webhook.
     * @param webhook the webhook to register
     */
    public void registerWebhook(Object webhook) {
        // Implement registration logic here
        log("Registering webhook: " + webhook);
        // Possibly store the webhook in a data store
    }

    /**
     * Dispatch the webhook event to the registered webhook.
     * @param webhook the webhook to dispatch to
     * @param event the event data
     */
    public void dispatchWebhook(Object webhook, Object event) {
        log("Dispatching webhook: " + webhook + " with event: " + event);
        // Use WebhookDispatcher to dispatch
        webhookDispatcher.dispatchWebhook((Webhook) webhook, event.toString());
    }

    /**
     * Retry the webhook dispatch in case of failure.
     * @param webhook the webhook to retry
     * @param event the event data
     * @param retryCount number of retry attempts
     */
    public void retryWebhook(Object webhook, Object event, int retryCount) {
        log("Retrying webhook dispatch: " + webhook + " attempt: " + retryCount);
        // Implement retry logic, possibly by re-dispatching
        for (int i = 0; i < retryCount; i++) {
            webhookDispatcher.dispatchWebhook((Webhook) webhook, event.toString());
        }
    }

    /**
     * Dispatch a new webhook.
     * @param newWebhook the new webhook to dispatch
     * @param idempotencyKey the idempotency key for the webhook
     */
    public void dispatchNewWebhook(NewWebhook newWebhook, String idempotencyKey) {
        log("Dispatching new webhook: " + newWebhook);
        // Assuming NewWebhook can be converted to Webhook for dispatching
        Webhook webhook = convertNewWebhookToWebhook(newWebhook);
        webhookDispatcher.dispatchWebhook(webhook, idempotencyKey);
    }

    /**
     * Convert NewWebhook to Webhook.
     * @param newWebhook the new webhook
     * @return converted Webhook
     */
    private Webhook convertNewWebhookToWebhook(NewWebhook newWebhook) {
        // Implement conversion logic here
        // Placeholder implementation
        return new Webhook(newWebhook.getPayload());
    }

    /**
     * Log webhook operations and events.
     * @param message the log message
     */
    public void log(String message) {
        System.out.println("WebhookService log: " + message);
    }

}
