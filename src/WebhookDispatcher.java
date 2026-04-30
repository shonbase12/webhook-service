import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WebhookDispatcher {
    private static final Logger logger = Logger.getLogger(WebhookDispatcher.class.getName());
    private int maxRetries;
    private static final long INITIAL_BACKOFF = 1000; // 1 second
    private static final long MAX_BACKOFF = 30000; // 30 seconds max backoff
    private static final long TIMEOUT = 5000; // 5 seconds timeout for sending webhook
    private static final long IDEMPOTENCY_KEY_TTL = 3600_000; // 1 hour TTL for idempotency keys

    private final Map<String, String> idempotencyStore = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> evictionTasks = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Constructor with dynamic maxRetries
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
                    scheduleEviction(idempotencyKey);
                    return;
                } catch (SpecificException e) {
                    logger.warning("Specific exception occurred: " + e.getMessage());
                } catch (TimeoutException e) {
                    logger.severe("Timeout occurred while sending webhook: " + e.getMessage());
                } catch (Exception e) {
                    logger.severe("An unexpected error occurred: " + e.getMessage());
                    logger.severe("Stack trace: ");
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.severe(element.toString());
                    }
                }

                attempt++;

                // Improved exponential backoff with full jitter
                long expBackoff = INITIAL_BACKOFF * (1L << (attempt - 1));
                long cappedBackoff = Math.min(expBackoff, MAX_BACKOFF);
                long jitter = (long) (random.nextDouble() * cappedBackoff);

                logger.info("Retrying dispatch... Attempt " + attempt + " of " + maxRetries + " in " + jitter + " ms.");

                try {
                    TimeUnit.MILLISECONDS.sleep(jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.severe("Thread interrupted during backoff: " + ie.getMessage());
                    return; // Abort retries if interrupted
                }
            }
            logger.severe("Max retries reached. Failed to dispatch webhook: " + webhook);
        });
    }

    private void scheduleEviction(String idempotencyKey) {
        // Cancel previous eviction if exists
        ScheduledFuture<?> previousTask = evictionTasks.put(idempotencyKey, scheduler.schedule(() -> {
            idempotencyStore.remove(idempotencyKey);
            evictionTasks.remove(idempotencyKey);
            logger.info("Evicted idempotency key from store: " + idempotencyKey);
        }, IDEMPOTENCY_KEY_TTL, TimeUnit.MILLISECONDS));

        if (previousTask != null) {
            previousTask.cancel(false);
        }
    }

    private void sendWebhookWithTimeout(Webhook webhook) throws SpecificException, TimeoutException {
        // Logic to send the webhook to the desired endpoint
        long startTime = System.currentTimeMillis();
        // Simulate sending the webhook (replace with actual logic)
        boolean success = sendToWebhookEndpoint(webhook);
        long elapsedTime = System.currentTimeMillis() - startTime;

        if (!success || elapsedTime > TIMEOUT) {
            throw new TimeoutException("Failed to send webhook or timeout exceeded.");
        }
    }

    private boolean validateWebhook(Webhook webhook) {
        // Implement enhanced webhook validation logic here
        // For example, checking required fields, signature verification, or payload schema validation
        if (webhook == null) {
            logger.warning("Webhook is null.");
            return false;
        }
        if (webhook.getPayload() == null || webhook.getPayload().isEmpty()) {
            logger.warning("Webhook payload is empty.");
            return false;
        }
        // Add more validation rules as needed
        return true;
    }

    private boolean sendToWebhookEndpoint(Webhook webhook) {
        // Enhanced webhook validation
        if (!validateWebhook(webhook)) {
            logger.severe("Webhook validation failed. Aborting send.");
            return false;
        }

        // Placeholder for actual sending logic
        // Implement the logic to send the webhook to your endpoint
        return true; // Simulate successful sending
    }
}
