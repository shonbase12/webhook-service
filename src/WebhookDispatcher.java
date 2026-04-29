import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebhookDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(WebhookDispatcher.class);
    private static final int MAX_RETRIES = 3; // Define max retries

    public void dispatchWebhook(Webhook webhook) {
        int retryCount = 0;
        boolean success = false;

        while (retryCount < MAX_RETRIES && !success) {
            try {
                // Code to send webhook...
                success = true; // Assuming send is successful
            } catch (Exception e) {
                logger.error("Failed to send webhook on attempt {}: {}", retryCount + 1, e.getMessage());
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    // Exponential backoff
                    try {
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000); // Backoff time in milliseconds
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // Restore interrupted status
                    }
                } else {
                    logger.error("Max retries reached for webhook: {}", webhook);
                    // Handle failure (e.g., log, alert, etc.)
                }
            }
        }
    }
}