import java.util.logging.Logger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class WebhookService {

    private static final Logger logger = Logger.getLogger(WebhookService.class.getName());
    private final WebhookDispatcher webhookDispatcher;
    private final Random random = new Random();
    private static final long INITIAL_BACKOFF = 1000; // 1 second
    private static final long MAX_BACKOFF = 30000; // 30 seconds max backoff

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
        logger.info("Registering webhook: " + webhook);
        // Possibly store the webhook in a data store
    }

    /**
     * Dispatch the webhook event to the registered webhook.
     * @param webhook the webhook to dispatch to
     * @param event the event data
     */
    public void dispatchWebhook(Object webhook, Object event) {
        logger.info("Dispatching webhook: " + webhook + " with event: " + event);
        // Use WebhookDispatcher to dispatch
        webhookDispatcher.dispatchWebhook((Webhook) webhook, event.toString());
    }

    /**
     * Retry the webhook dispatch in case of failure with exponential backoff and jitter.
     * @param webhook the webhook to retry
     * @param event the event data
     * @param retryCount number of retry attempts
     */
    public void retryWebhook(Object webhook, Object event, int retryCount) {
        logger.info("Retrying webhook dispatch: " + webhook + " attempt: " + retryCount);

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            webhookDispatcher.dispatchWebhook((Webhook) webhook, event.toString());

            // Exponential backoff with full jitter
            long expBackoff = INITIAL_BACKOFF * (1L << (attempt - 1));
            long cappedBackoff = Math.min(expBackoff, MAX_BACKOFF);
            long jitter = (long) (random.nextDouble() * cappedBackoff);

            logger.info("Backoff for " + jitter + " ms before next retry.");

            try {
                TimeUnit.MILLISECONDS.sleep(jitter);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.severe("Thread interrupted during backoff: " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Dispatch a new webhook.
     * @param newWebhook the new webhook to dispatch
     * @param idempotencyKey the idempotency key for the webhook
     */
    public void dispatchNewWebhook(NewWebhook newWebhook, String idempotencyKey) {
        logger.info("Dispatching new webhook: " + newWebhook);
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

}
