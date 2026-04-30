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

// Additional imports for enhancements
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class WebhookDispatcher {
    private static final Logger logger = Logger.getLogger(WebhookDispatcher.class.getName());

    private int maxRetries;
    private BackoffStrategy backoffStrategy;
    private Predicate<Exception> retryCondition;

    private static final long TIMEOUT = 5000; // 5 seconds timeout for sending webhook

    private long idempotencyKeyTTL;

    // Simulated persistent store for idempotency keys (replace with actual persistent store)
    private final Map<String, String> idempotencyStore = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> evictionTasks = new ConcurrentHashMap<>();

    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final int circuitBreakerThreshold = 5;
    private int failureCount = 0;

    // Constructor with dynamic maxRetries and configurable backoff
    public WebhookDispatcher(int maxRetries, long idempotencyKeyTTL, BackoffStrategy backoffStrategy, Predicate<Exception> retryCondition) {
        this.maxRetries = maxRetries;
        this.idempotencyKeyTTL = idempotencyKeyTTL;
        this.backoffStrategy = backoffStrategy;
        this.retryCondition = retryCondition;
    }

    public void dispatchWebhook(Webhook webhook, String idempotencyKey) {
        if (!validateIdempotencyKey(idempotencyKey)) {
            logger.warning("Invalid idempotency key. Aborting dispatch.");
            return;
        }

        if (idempotencyStore.containsKey(idempotencyKey)) {
            logger.info("Webhook with idempotency key " + idempotencyKey + " has already been processed.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            if (circuitOpen.get()) {
                logger.warning("Circuit breaker is open. Aborting dispatch.");
                return;
            }

            int attempt = 0;
            while (attempt < maxRetries) {
                try {
                    logger.info("Preparing to dispatch webhook: " + webhook);
                    sendWebhookWithTimeout(webhook);
                    logger.info("Webhook dispatched successfully.");
                    idempotencyStore.put(idempotencyKey, "sent");
                    scheduleEviction(idempotencyKey);
                    resetCircuitBreaker();
                    return;
                } catch (Exception e) {
                    if (!retryCondition.test(e)) {
                        logger.severe("Non-retryable exception occurred: " + e.getMessage());
                        break;
                    }

                    failureCount++;
                    if (failureCount >= circuitBreakerThreshold) {
                        openCircuitBreaker();
                        logger.severe("Circuit breaker opened due to repeated failures.");
                        break;
                    }

                    logger.warning("Exception occurred: " + e.getMessage());
                }

                attempt++;

                long backoffTime = backoffStrategy.getBackoffTime(attempt);
                logger.info("Retrying dispatch... Attempt " + attempt + " of " + maxRetries + " in " + backoffTime + " ms.");

                try {
                    TimeUnit.MILLISECONDS.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.severe("Thread interrupted during backoff: " + ie.getMessage());
                    return; // Abort retries if interrupted
                }
            }

            logger.severe("Max retries reached. Failed to dispatch webhook: " + webhook);
            // Here, implement fallback or dead-letter queue logic as needed
        });
    }

    private void scheduleEviction(String idempotencyKey) {
        ScheduledFuture<?> previousTask = evictionTasks.put(idempotencyKey, scheduler.schedule(() -> {
            idempotencyStore.remove(idempotencyKey);
            evictionTasks.remove(idempotencyKey);
            logger.info("Evicted idempotency key from store: " + idempotencyKey);
        }, idempotencyKeyTTL, TimeUnit.MILLISECONDS));

        if (previousTask != null) {
            previousTask.cancel(false);
        }
    }

    private void sendWebhookWithTimeout(Webhook webhook) throws Exception {
        long startTime = System.currentTimeMillis();
        boolean success = sendToWebhookEndpoint(webhook);
        long elapsedTime = System.currentTimeMillis() - startTime;

        if (!success || elapsedTime > TIMEOUT) {
            throw new TimeoutException("Failed to send webhook or timeout exceeded.");
        }
    }

    private boolean validateWebhook(Webhook webhook) {
        if (webhook == null) {
            logger.warning("Webhook is null.");
            return false;
        }
        if (webhook.getPayload() == null || webhook.getPayload().isEmpty()) {
            logger.warning("Webhook payload is empty.");
            return false;
        }
        // Additional validation rules can be added here
        return true;
    }

    private boolean sendToWebhookEndpoint(Webhook webhook) {
        if (!validateWebhook(webhook)) {
            logger.severe("Webhook validation failed. Aborting send.");
            return false;
        }
        // Placeholder for actual sending logic
        return true; // Simulate successful sending
    }

    private boolean validateIdempotencyKey(String key) {
        // Simple validation example: non-null, non-empty, length limit
        return key != null && !key.trim().isEmpty() && key.length() <= 255;
    }

    private void openCircuitBreaker() {
        circuitOpen.set(true);
        scheduler.schedule(this::resetCircuitBreaker, 1, TimeUnit.MINUTES); // Open for 1 minute
    }

    private void resetCircuitBreaker() {
        circuitOpen.set(false);
        failureCount = 0;
        logger.info("Circuit breaker reset.");
    }

    // Backoff strategy interface and implementations
    public interface BackoffStrategy {
        long getBackoffTime(int attempt);
    }

    public static class ExponentialBackoffWithJitter implements BackoffStrategy {
        private final long initialBackoff;
        private final long maxBackoff;
        private final Random random = new Random();

        public ExponentialBackoffWithJitter(long initialBackoff, long maxBackoff) {
            this.initialBackoff = initialBackoff;
            this.maxBackoff = maxBackoff;
        }

        @Override
        public long getBackoffTime(int attempt) {
            long expBackoff = initialBackoff * (1L << (attempt - 1));
            long cappedBackoff = Math.min(expBackoff, maxBackoff);
            return (long) (random.nextDouble() * cappedBackoff);
        }
    }

    // Example retry condition based on exception type
    public static Predicate<Exception> defaultRetryCondition = (Exception e) -> {
        // Retry on TimeoutException and SpecificException (assume these are defined elsewhere)
        return e instanceof TimeoutException || e instanceof SpecificException;
    };

    // Exception classes (to be defined or imported properly)
    public static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }

    public static class SpecificException extends Exception {
        public SpecificException(String message) {
            super(message);
        }
    }
}
