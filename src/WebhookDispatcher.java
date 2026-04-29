import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class WebhookDispatcher {
    private static final Logger logger = Logger.getLogger(WebhookDispatcher.class.getName());
    private int maxRetries;
    private static final long INITIAL_BACKOFF = 1000; // 1 second
    private static final long TIMEOUT = 5000; // 5 seconds timeout for sending webhook
    private Map<String, String> idempotencyStore = new ConcurrentHashMap<>();

    public WebhookDispatcher(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void dispatchWebhook(Webhook webhook, String idempotencyKey) {
        if (idempotencyStore.containsKey(idempotencyKey)) {
            logger.info("Webhook with idempotency key " + idempotencyKey + " has already been processed.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            int attempt = 0;
            while (attempt < maxRetries) {
                try {
                    logger.info("Preparing to dispatch webhook: " + webhook);
                    // Logic to send the webhook with timeout
                    sendWebhookWithTimeout(webhook);
                    logger.info("Webhook dispatched successfully.");
                    idempotencyStore.put(idempotencyKey, "sent");
                    return;
                } catch (SpecificException e) {
                    logger.warning("Specific exception occurred: " + e.getMessage());
                } catch (Exception e) {
                    logger.severe("An error occurred: " + e.getMessage());
                    logger.severe("Stack trace: ");
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.severe(element.toString());
                    }
                }

                attempt++;
                long backoffTime = INITIAL_BACKOFF * (1 << (attempt - 1)); // Exponential backoff
                logger.info("Retrying dispatch... Attempt " + attempt + " of " + maxRetries + " in " + backoffTime + " ms.");
                try {
                    TimeUnit.MILLISECONDS.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.severe("Thread interrupted during backoff: " + ie.getMessage());
                }
            }
            logger.severe("Max retries reached. Failed to dispatch webhook: " + webhook);
        });
    }

    private void sendWebhookWithTimeout(Webhook webhook) throws SpecificException {
        // Implementation for sending the webhook with timeout
        // This method will check if sending the webhook exceeds the TIMEOUT limit.
        // If it does, it should throw a SpecificException to trigger retry logic.
    }
}