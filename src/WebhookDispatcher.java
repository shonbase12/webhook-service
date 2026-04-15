import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class WebhookDispatcher {
    private static final Logger logger = Logger.getLogger(WebhookDispatcher.class.getName());
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF = 1000; // 1 second

    public void dispatchWebhook(Webhook webhook) {
        CompletableFuture.runAsync(() -> {
            int attempt = 0;
            while (attempt < MAX_RETRIES) {
                try {
                    // Logic to send the webhook
                    sendWebhook(webhook);
                    logger.info("Webhook dispatched successfully.");
                    return;
                } catch (SpecificException e) {
                    logger.warning("Specific exception occurred: " + e.getMessage());
                } catch (Exception e) {
                    logger.severe("An error occurred: " + e.getMessage());
                }

                attempt++;
                long backoffTime = INITIAL_BACKOFF * (1 << (attempt - 1)); // Exponential backoff
                logger.info("Retrying... Attempt " + attempt + " in " + backoffTime + " ms");
                try {
                    TimeUnit.MILLISECONDS.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.severe("Thread interrupted during backoff: " + ie.getMessage());
                }
            }
            logger.severe("Max retries reached. Failed to dispatch webhook.");
        });
    }

    private void sendWebhook(Webhook webhook) throws SpecificException {
        // Implementation for sending the webhook
    }
}
