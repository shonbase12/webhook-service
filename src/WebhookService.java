import java.util.logging.Logger;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class WebhookService {

    private static final Logger logger = Logger.getLogger(WebhookService.class.getName());
    private final WebhookDispatcher webhookDispatcher;
    private final Random random = new Random();
    private static final long INITIAL_BACKOFF = 1000; // 1 second
    private static final long MAX_BACKOFF = 30000; // 30 seconds max backoff
    private final Map<Object, Webhook> registeredWebhooks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public WebhookService() {
        // Initialize WebhookDispatcher with a default maxRetries value, e.g., 3
        this.webhookDispatcher = new WebhookDispatcher(3);
    }

    /**
     * Register a new webhook.
     * @param webhook the webhook to register
     */
    public void registerWebhook(Webhook webhook) {
        if (webhook == null) {
            logger.warning("Attempted to register a null webhook.");
            return;
        }
        registeredWebhooks.put(webhook.getId(), webhook);
        logger.info("Registered webhook with ID: " + webhook.getId());
    }

    /**
     * Dispatch the webhook event to the registered webhook.
     * @param webhook the webhook to dispatch to
     * @param event the event data
     */
    public void dispatchWebhook(Webhook webhook, Object event) {
        if (webhook == null) {
            logger.warning("Attempted to dispatch a null webhook.");
            return;
        }
        if (!registeredWebhooks.containsKey(webhook.getId())) {
            logger.warning("Webhook with ID " + webhook.getId() + " is not registered.");
            return;
        }
        try {
            logger.info("Dispatching webhook ID: " + webhook.getId() + " with event: " + event);
            webhookDispatcher.dispatchWebhook(webhook, event.toString());
        } catch (Exception e) {
            logger.severe("Failed to dispatch webhook ID: " + webhook.getId() + ". Error: " + e.getMessage());
            // Optionally rethrow or handle retry externally
        }
    }

    /**
     * Retry the webhook dispatch in case of failure with exponential backoff and jitter.
     * @param webhook the webhook to retry
     * @param event the event data
     * @param maxRetryAttempts maximum number of retry attempts
     */
    public void retryWebhook(Webhook webhook, Object event, int maxRetryAttempts) {
        if (webhook == null) {
            logger.warning("Attempted to retry a null webhook.");
            return;
        }
        logger.info("Retrying webhook dispatch ID: " + webhook.getId() + " with max attempts: " + maxRetryAttempts);
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                webhookDispatcher.dispatchWebhook(webhook, event.toString());
                logger.info("Successfully dispatched webhook ID: " + webhook.getId() + " on attempt " + attempt);
                break; // success, exit retry loop
            } catch (Exception e) {
                logger.warning("Attempt " + attempt + " failed for webhook ID: " + webhook.getId() + ". Error: " + e.getMessage());
                if (attempt == maxRetryAttempts) {
                    logger.severe("Max retry attempts reached for webhook ID: " + webhook.getId());
                    break;
                }
                // Exponential backoff with full jitter
                long expBackoff = INITIAL_BACKOFF * (1L << (attempt - 1));
                long cappedBackoff = Math.min(expBackoff, MAX_BACKOFF);
                long jitter = (long) (random.nextDouble() * cappedBackoff);
                logger.info("Backing off for " + jitter + " ms before next retry.");
                try {
                    TimeUnit.MILLISECONDS.sleep(jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.severe("Thread interrupted during backoff for webhook ID: " + webhook.getId());
                    break;
                }
            }
        }
    }

    /**
     * Dispatch a new webhook asynchronously.
     * @param newWebhook the new webhook to dispatch
     * @param idempotencyKey the idempotency key for the webhook
     */
    public void dispatchNewWebhook(NewWebhook newWebhook, String idempotencyKey) {
        if (newWebhook == null) {
            logger.warning("Attempted to dispatch a null newWebhook.");
            return;
        }
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            logger.warning("Idempotency key is required to dispatch new webhook.");
            return;
        }
        logger.info("Asynchronously dispatching new webhook with idempotency key: " + idempotencyKey);
        executorService.submit(() -> {
            try {
                Webhook webhook = convertNewWebhookToWebhook(newWebhook);
                // Register the new webhook before dispatching
                registerWebhook(webhook);
                // Dispatch with idempotency key
                webhookDispatcher.dispatchWebhook(webhook, idempotencyKey);
            } catch (Exception e) {
                logger.severe("Failed to asynchronously dispatch new webhook. Error: " + e.getMessage());
            }
        });
    }

    /**
     * Convert NewWebhook to Webhook.
     * @param newWebhook the new webhook
     * @return converted Webhook
     */
    private Webhook convertNewWebhookToWebhook(NewWebhook newWebhook) {
        // Implement conversion logic here
        // Example placeholder implementation assuming NewWebhook has getPayload() and getId()
        Webhook webhook = new Webhook(newWebhook.getPayload());
        webhook.setId(newWebhook.getId());
        return webhook;
    }

}
